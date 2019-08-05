package com.jstarcraft.rns.recommend.collaborative;

import com.google.common.collect.HashBasedTable;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.configure.Configurator;
import com.jstarcraft.rns.recommend.ProbabilisticGraphicalRecommender;
import com.jstarcraft.rns.utility.GammaUtility;
import com.jstarcraft.rns.utility.SampleUtility;

import it.unimi.dsi.fastutil.ints.Int2IntRBTreeMap;

/**
 * 
 * BUCM推荐器
 * 
 * <pre>
 * Bayesian User Community Model
 * Modeling Item Selection and Relevance for Accurate Recommendations
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public abstract class BUCMRecommender extends ProbabilisticGraphicalRecommender {
    /**
     * number of occurrences of entry (t, i, r)
     */
    private int[][][] topicItemRateNumbers;

    /**
     * number of occurrentces of entry (user, topic)
     */
    private DenseMatrix userTopicNumbers;

    /**
     * number of occurences of users
     */
    private DenseVector userNumbers;

    /**
     * number of occurrences of entry (topic, item)
     */
    private DenseMatrix topicItemNumbers;

    /**
     * number of occurrences of items
     */
    private DenseVector topicNumbers;

    /**
     * cumulative statistics of probabilities of (t, i, r)
     */
    private float[][][] topicItemRateSums;

    /**
     * posterior probabilities of parameters epsilon_{k, i, r}
     */
    protected float[][][] topicItemRateProbabilities;

    /**
     * P(k | u)
     */
    protected DenseMatrix userTopicProbabilities, userTopicSums;

    /**
     * P(i | k)
     */
    protected DenseMatrix topicItemProbabilities, topicItemSums;

    /**
     *
     */
    private DenseVector alpha;

    /**
     *
     */
    private DenseVector beta;

    /**
     *
     */
    private DenseVector gamma;

    /**
     *
     */
    protected Int2IntRBTreeMap topicAssignments;

    private DenseVector probabilities;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        // cumulative parameters
        // TODO 考虑重构
        userTopicSums = DenseMatrix.valueOf(userSize, factorSize);
        topicItemSums = DenseMatrix.valueOf(factorSize, itemSize);
        topicItemRateSums = new float[factorSize][itemSize][scoreSize];

        // initialize count varialbes
        userTopicNumbers = DenseMatrix.valueOf(userSize, factorSize);
        userNumbers = DenseVector.valueOf(userSize);

        topicItemNumbers = DenseMatrix.valueOf(factorSize, itemSize);
        topicNumbers = DenseVector.valueOf(factorSize);

        topicItemRateNumbers = new int[factorSize][itemSize][scoreSize];

        float initAlpha = configuration.getFloat("recommender.bucm.alpha", 1F / factorSize);
        alpha = DenseVector.valueOf(factorSize);
        alpha.setValues(initAlpha);

        float initBeta = configuration.getFloat("re.bucm.beta", 1F / itemSize);
        beta = DenseVector.valueOf(itemSize);
        beta.setValues(initBeta);

        float initGamma = configuration.getFloat("recommender.bucm.gamma", 1F / factorSize);
        gamma = DenseVector.valueOf(scoreSize);
        gamma.setValues(initGamma);

        // initialize topics
        topicAssignments = new Int2IntRBTreeMap();
        for (MatrixScalar term : scoreMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            float rate = term.getValue();
            int rateIndex = scoreIndexes.get(rate); // rating level 0 ~
                                                    // numLevels
            int topicIndex = RandomUtility.randomInteger(factorSize); // 0 ~
            // k-1

            // Assign a topic t to pair (u, i)
            topicAssignments.put(userIndex * itemSize + itemIndex, topicIndex);
            // for users
            userTopicNumbers.shiftValue(userIndex, topicIndex, 1F);
            userNumbers.shiftValue(userIndex, 1F);

            // for items
            topicItemNumbers.shiftValue(topicIndex, itemIndex, 1F);
            topicNumbers.shiftValue(topicIndex, 1F);

            // for ratings
            topicItemRateNumbers[topicIndex][itemIndex][rateIndex]++;
        }

        probabilities = DenseVector.valueOf(factorSize);
    }

    @Override
    protected void eStep() {
        float alphaSum = alpha.getSum(false);
        float betaSum = beta.getSum(false);
        float gammaSum = gamma.getSum(false);

        // collapse Gibbs sampling
        for (MatrixScalar term : scoreMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            float rate = term.getValue();
            int rateIndex = scoreIndexes.get(rate); // rating level 0 ~
                                                    // numLevels
            int topicIndex = topicAssignments.get(userIndex * itemSize + itemIndex);

            // for user
            userTopicNumbers.shiftValue(userIndex, topicIndex, -1F);
            userNumbers.shiftValue(userIndex, -1F);

            // for item
            topicItemNumbers.shiftValue(topicIndex, itemIndex, -1F);
            topicNumbers.shiftValue(topicIndex, -1F);

            // for rating
            topicItemRateNumbers[topicIndex][itemIndex][rateIndex]--;

            // do multinomial sampling via cumulative method:
            // 计算概率
            DefaultScalar sum = DefaultScalar.getInstance();
            sum.setValue(0F);
            probabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                int index = scalar.getIndex();
                float value = (userTopicNumbers.getValue(userIndex, index) + alpha.getValue(index)) / (userNumbers.getValue(userIndex) + alphaSum);
                value *= (topicItemNumbers.getValue(index, itemIndex) + beta.getValue(itemIndex)) / (topicNumbers.getValue(index) + betaSum);
                value *= (topicItemRateNumbers[index][itemIndex][rateIndex] + gamma.getValue(rateIndex)) / (topicItemNumbers.getValue(index, itemIndex) + gammaSum);
                sum.shiftValue(value);
                scalar.setValue(sum.getValue());
            });
            topicIndex = SampleUtility.binarySearch(probabilities, 0, probabilities.getElementSize() - 1, RandomUtility.randomFloat(sum.getValue()));

            // new topic t
            topicAssignments.put(userIndex * itemSize + itemIndex, topicIndex);

            // add newly estimated z_i to count variables
            userTopicNumbers.shiftValue(userIndex, topicIndex, 1F);
            userNumbers.shiftValue(userIndex, 1F);

            topicItemNumbers.shiftValue(topicIndex, itemIndex, 1F);
            topicNumbers.shiftValue(topicIndex, 1F);

            topicItemRateNumbers[topicIndex][itemIndex][rateIndex]++;
        }
    }

    /**
     * Thomas P. Minka, Estimating a Dirichlet distribution, see Eq.(55)
     */
    @Override
    protected void mStep() {
        float denominator;
        float value = 0F;

        // update alpha
        float alphaValue;
        float alphaSum = alpha.getSum(false);
        float alphaDigamma = GammaUtility.digamma(alphaSum);
        denominator = 0F;
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            value = userNumbers.getValue(userIndex);
            if (value != 0F) {
                denominator += GammaUtility.digamma(value + alphaSum) - alphaDigamma;
            }
        }
        for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
            alphaValue = alpha.getValue(topicIndex);
            alphaDigamma = GammaUtility.digamma(alphaValue);
            float numerator = 0F;
            for (int userIndex = 0; userIndex < userSize; userIndex++) {
                value = userTopicNumbers.getValue(userIndex, topicIndex);
                if (value != 0F) {
                    numerator += GammaUtility.digamma(value + alphaValue) - alphaDigamma;
                }
            }
            if (numerator != 0F) {
                alpha.setValue(topicIndex, alphaValue * (numerator / denominator));
            }
        }

        // update beta
        float betaValue;
        float bataSum = beta.getSum(false);
        float betaDigamma = GammaUtility.digamma(bataSum);
        denominator = 0F;
        for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
            value = topicNumbers.getValue(topicIndex);
            if (value != 0F) {
                denominator += GammaUtility.digamma(value + bataSum) - betaDigamma;
            }
        }
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            betaValue = beta.getValue(itemIndex);
            betaDigamma = GammaUtility.digamma(betaValue);
            float numerator = 0F;
            for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                value = topicItemNumbers.getValue(topicIndex, itemIndex);
                if (value != 0F) {
                    numerator += GammaUtility.digamma(value + betaValue) - betaDigamma;
                }
            }
            if (numerator != 0F) {
                beta.setValue(itemIndex, betaValue * (numerator / denominator));
            }
        }

        // update gamma
        float gammaValue;
        float gammaSum = gamma.getSum(false);
        float gammaDigamma = GammaUtility.digamma(gammaSum);
        denominator = 0F;
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                value = topicItemNumbers.getValue(topicIndex, itemIndex);
                if (value != 0F) {
                    denominator += GammaUtility.digamma(value + gammaSum) - gammaDigamma;
                }
            }
        }
        for (int rateIndex = 0; rateIndex < scoreSize; rateIndex++) {
            gammaValue = gamma.getValue(rateIndex);
            gammaDigamma = GammaUtility.digamma(gammaValue);
            float numerator = 0F;
            for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                    value = topicItemRateNumbers[topicIndex][itemIndex][rateIndex];
                    if (value != 0F) {
                        numerator += GammaUtility.digamma(value + gammaValue) - gammaDigamma;
                    }
                }
            }
            if (numerator != 0F) {
                gamma.setValue(rateIndex, gammaValue * (numerator / denominator));
            }
        }
    }

    @Override
    protected boolean isConverged(int iter) {
        float loss = 0F;
        // get params
        estimateParameters();
        // compute likelihood
        int sum = 0;
        for (MatrixScalar term : scoreMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            float rate = term.getValue();
            int rateIndex = scoreIndexes.get(rate);
            float probability = 0F;
            for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                probability += userTopicProbabilities.getValue(userIndex, topicIndex) * topicItemProbabilities.getValue(topicIndex, itemIndex) * topicItemRateProbabilities[topicIndex][itemIndex][rateIndex];
            }
            loss += (float) -Math.log(probability);
            sum++;
        }
        loss /= sum;
        float delta = loss - currentError; // loss gets smaller, delta <= 0
        if (numberOfStatistics > 1 && delta > 0) {
            return true;
        }
        currentError = loss;
        return false;
    }

    protected void readoutParameters() {
        float value;
        float sumAlpha = alpha.getSum(false);
        float sumBeta = beta.getSum(false);
        float sumGamma = gamma.getSum(false);

        for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
            for (int userIndex = 0; userIndex < userSize; userIndex++) {
                value = (userTopicNumbers.getValue(userIndex, topicIndex) + alpha.getValue(topicIndex)) / (userNumbers.getValue(userIndex) + sumAlpha);
                userTopicSums.shiftValue(userIndex, topicIndex, value);
            }
            for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                value = (topicItemNumbers.getValue(topicIndex, itemIndex) + beta.getValue(itemIndex)) / (topicNumbers.getValue(topicIndex) + sumBeta);
                topicItemSums.shiftValue(topicIndex, itemIndex, value);
                for (int rateIndex = 0; rateIndex < scoreSize; rateIndex++) {
                    value = (topicItemRateNumbers[topicIndex][itemIndex][rateIndex] + gamma.getValue(rateIndex)) / (topicItemNumbers.getValue(topicIndex, itemIndex) + sumGamma);
                    topicItemRateSums[topicIndex][itemIndex][rateIndex] += value;
                }
            }
        }
        numberOfStatistics++;
    }

    @Override
    protected void estimateParameters() {
        userTopicProbabilities = DenseMatrix.copyOf(userTopicSums);
        userTopicProbabilities.scaleValues(1F / numberOfStatistics);
        topicItemProbabilities = DenseMatrix.copyOf(topicItemSums);
        topicItemProbabilities.scaleValues(1F / numberOfStatistics);

        topicItemRateProbabilities = new float[factorSize][itemSize][scoreSize];
        for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
            for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                for (int rateIndex = 0; rateIndex < scoreSize; rateIndex++) {
                    topicItemRateProbabilities[topicIndex][itemIndex][rateIndex] = topicItemRateSums[topicIndex][itemIndex][rateIndex] / numberOfStatistics;
                }
            }
        }
    }

}
