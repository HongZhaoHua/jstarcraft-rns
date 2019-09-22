package com.jstarcraft.rns.model.collaborative.rating;

import java.util.Map.Entry;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.HashMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.model.ProbabilisticGraphicalModel;
import com.jstarcraft.rns.utility.GammaUtility;
import com.jstarcraft.rns.utility.SampleUtility;

import it.unimi.dsi.fastutil.ints.Int2IntRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;

/**
 * 
 * URP推荐器
 * 
 * <pre>
 * User Rating Profile: a LDA model for rating prediction
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class URPModel extends ProbabilisticGraphicalModel {

    private float preRMSE;

    /**
     * number of occurrentces of entry (user, topic)
     */
    private DenseMatrix userTopicTimes;

    /**
     * number of occurences of users
     */
    private DenseVector userTopicNumbers;

    /**
     * number of occurrences of entry (topic, item)
     */
    private DenseMatrix topicItemNumbers;

    /**
     * P(k | u)
     */
    private DenseMatrix userTopicProbabilities, userTopicSums;

    /**
     * user parameters
     */
    private DenseVector alpha;

    /**
     * item parameters
     */
    private DenseVector beta;

    /**
     *
     */
    private Int2IntRBTreeMap topicAssignments;

    /**
     * number of occurrences of entry (t, i, r)
     */
    private int[][][] topicItemTimes; // Nkir

    /**
     * cumulative statistics of probabilities of (t, i, r)
     */
    private float[][][] topicItemScoreSums; // PkirSum;

    /**
     * posterior probabilities of parameters phi_{k, i, r}
     */
    private float[][][] topicItemScoreProbabilities; // Pkir;

    private DenseVector randomProbabilities;

    /** 学习矩阵与校验矩阵(TODO 将scoreMatrix划分) */
    private SparseMatrix learnMatrix, checkMatrix;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);

        float checkRatio = configuration.getFloat("recommender.urp.chech.ratio", 0F);
        if (checkRatio == 0F) {
            learnMatrix = scoreMatrix;
            checkMatrix = null;
        } else {
            HashMatrix learnTable = new HashMatrix(true, userSize, itemSize, new Long2FloatRBTreeMap());
            HashMatrix checkTable = new HashMatrix(true, userSize, itemSize, new Long2FloatRBTreeMap());
            for (MatrixScalar term : scoreMatrix) {
                int userIndex = term.getRow();
                int itemIndex = term.getColumn();
                float score = term.getValue();
                if (RandomUtility.randomFloat(1F) <= checkRatio) {
                    checkTable.setValue(userIndex, itemIndex, score);
                } else {
                    learnTable.setValue(userIndex, itemIndex, score);
                }
            }
            learnMatrix = SparseMatrix.valueOf(userSize, itemSize, learnTable);
            checkMatrix = SparseMatrix.valueOf(userSize, itemSize, checkTable);
        }

        // cumulative parameters
        userTopicSums = DenseMatrix.valueOf(userSize, factorSize);
        topicItemScoreSums = new float[factorSize][itemSize][scoreSize];

        // initialize count variables
        userTopicTimes = DenseMatrix.valueOf(userSize, factorSize);
        userTopicNumbers = DenseVector.valueOf(userSize);

        topicItemTimes = new int[factorSize][itemSize][scoreSize];
        topicItemNumbers = DenseMatrix.valueOf(factorSize, itemSize);

        float initAlpha = configuration.getFloat("recommender.pgm.bucm.alpha", 1F / factorSize);
        alpha = DenseVector.valueOf(factorSize);
        alpha.setValues(initAlpha);

        float initBeta = configuration.getFloat("recommender.pgm.bucm.beta", 1F / factorSize);
        beta = DenseVector.valueOf(scoreSize);
        beta.setValues(initBeta);

        // initialize topics
        topicAssignments = new Int2IntRBTreeMap();
        for (MatrixScalar term : learnMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            float score = term.getValue();
            int scoreIndex = scoreIndexes.get(score); // rating level 0 ~
                                                      // numLevels
            int topicIndex = RandomUtility.randomInteger(factorSize); // 0
                                                                      // ~
            // k-1

            // Assign a topic t to pair (u, i)
            topicAssignments.put(userIndex * itemSize + itemIndex, topicIndex);
            // number of pairs (u, t) in (u, i, t)
            userTopicTimes.shiftValue(userIndex, topicIndex, 1);
            // total number of items of user u
            userTopicNumbers.shiftValue(userIndex, 1);

            // number of pairs (t, i, r)
            topicItemTimes[topicIndex][itemIndex][scoreIndex]++;
            // total number of words assigned to topic t
            topicItemNumbers.shiftValue(topicIndex, itemIndex, 1);
        }

        randomProbabilities = DenseVector.valueOf(factorSize);
    }

    @Override
    protected void eStep() {
        float sumAlpha = alpha.getSum(false);
        float sumBeta = beta.getSum(false);

        // collapse Gibbs sampling
        for (MatrixScalar term : learnMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            float score = term.getValue();
            int scoreIndex = scoreIndexes.get(score); // rating level 0 ~
                                                      // numLevels
            int assignmentIndex = topicAssignments.get(userIndex * itemSize + itemIndex);

            userTopicTimes.shiftValue(userIndex, assignmentIndex, -1);
            userTopicNumbers.shiftValue(userIndex, -1);
            topicItemTimes[assignmentIndex][itemIndex][scoreIndex]--;
            topicItemNumbers.shiftValue(assignmentIndex, itemIndex, -1);

            // 计算概率
            DefaultScalar sum = DefaultScalar.getInstance();
            sum.setValue(0F);
            randomProbabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                int index = scalar.getIndex();
                float value = (userTopicTimes.getValue(userIndex, index) + alpha.getValue(index)) / (userTopicNumbers.getValue(userIndex) + sumAlpha) * (topicItemTimes[index][itemIndex][scoreIndex] + beta.getValue(scoreIndex)) / (topicItemNumbers.getValue(index, itemIndex) + sumBeta);
                sum.shiftValue(value);
                scalar.setValue(sum.getValue());
            });
            assignmentIndex = SampleUtility.binarySearch(randomProbabilities, 0, randomProbabilities.getElementSize() - 1, RandomUtility.randomFloat(sum.getValue()));

            // new topic t
            topicAssignments.put(userIndex * itemSize + itemIndex, assignmentIndex);

            // add newly estimated z_i to count variables
            userTopicTimes.shiftValue(userIndex, assignmentIndex, 1);
            userTopicNumbers.shiftValue(userIndex, 1);
            topicItemTimes[assignmentIndex][itemIndex][scoreIndex]++;
            topicItemNumbers.shiftValue(assignmentIndex, itemIndex, 1);
        }

    }

    /**
     * Thomas P. Minka, Estimating a Dirichlet distribution, see Eq.(55)
     */
    @Override
    protected void mStep() {
        float denominator;
        float value;

        // update alpha vector
        float alphaSum = alpha.getSum(false);
        float alphaDigamma = GammaUtility.digamma(alphaSum);
        float alphaValue;
        denominator = 0F;
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            value = userTopicNumbers.getValue(userIndex);
            if (value != 0F) {
                denominator += GammaUtility.digamma(value + alphaSum) - alphaDigamma;
            }
        }
        for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
            alphaValue = alpha.getValue(topicIndex);
            alphaDigamma = GammaUtility.digamma(alphaValue);
            float numerator = 0F;
            for (int userIndex = 0; userIndex < userSize; userIndex++) {
                value = userTopicTimes.getValue(userIndex, topicIndex);
                if (value != 0F) {
                    numerator += GammaUtility.digamma(value + alphaValue) - alphaDigamma;
                }
            }
            if (numerator != 0F) {
                alpha.setValue(topicIndex, alphaValue * (numerator / denominator));
            }
        }

        // update beta_k
        float betaSum = beta.getSum(false);
        float betaDigamma = GammaUtility.digamma(betaSum);
        float betaValue;
        denominator = 0F;
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                value = topicItemNumbers.getValue(topicIndex, itemIndex);
                if (value != 0F) {
                    denominator += GammaUtility.digamma(value + betaSum) - betaDigamma;
                }
            }
        }
        for (int scoreIndex = 0; scoreIndex < scoreSize; scoreIndex++) {
            betaValue = beta.getValue(scoreIndex);
            betaDigamma = GammaUtility.digamma(betaValue);
            float numerator = 0F;
            for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                    value = topicItemTimes[topicIndex][itemIndex][scoreIndex];
                    if (value != 0F) {
                        numerator += GammaUtility.digamma(value + betaValue) - betaDigamma;
                    }
                }
            }
            if (numerator != 0F) {
                beta.setValue(scoreIndex, betaValue * (numerator / denominator));
            }
        }
    }

    protected void readoutParameters() {
        float value = 0F;
        float sumAlpha = alpha.getSum(false);
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                value = (userTopicTimes.getValue(userIndex, topicIndex) + alpha.getValue(topicIndex)) / (userTopicNumbers.getValue(userIndex) + sumAlpha);
                userTopicSums.shiftValue(userIndex, topicIndex, value);
            }
        }
        float sumBeta = beta.getSum(false);
        for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
            for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                for (int scoreIndex = 0; scoreIndex < scoreSize; scoreIndex++) {
                    value = (topicItemTimes[topicIndex][itemIndex][scoreIndex] + beta.getValue(scoreIndex)) / (topicItemNumbers.getValue(topicIndex, itemIndex) + sumBeta);
                    topicItemScoreSums[topicIndex][itemIndex][scoreIndex] += value;
                }
            }
        }
        numberOfStatistics++;
    }

    @Override
    protected void estimateParameters() {
        userTopicProbabilities = DenseMatrix.copyOf(userTopicSums);
        userTopicProbabilities.scaleValues(1F / numberOfStatistics);
        topicItemScoreProbabilities = new float[factorSize][itemSize][scoreSize];
        for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
            for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                for (int scoreIndex = 0; scoreIndex < scoreSize; scoreIndex++) {
                    topicItemScoreProbabilities[topicIndex][itemIndex][scoreIndex] = topicItemScoreSums[topicIndex][itemIndex][scoreIndex] / numberOfStatistics;
                }
            }
        }
    }

    @Override
    protected boolean isConverged(int iter) {
        // TODO 此处使用validMatrix似乎更合理.
        if (checkMatrix == null) {
            return false;
        }
        // get posterior probability distribution first
        estimateParameters();
        // compute current RMSE
        int count = 0;
        float sum = 0F;
        // TODO 此处使用validMatrix似乎更合理.
        for (MatrixScalar term : checkMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            float score = term.getValue();
            float predict = predict(userIndex, itemIndex);
            if (Float.isNaN(predict)) {
                continue;
            }
            float error = score - predict;
            sum += error * error;
            count++;
        }
        float rmse = (float) Math.sqrt(sum / count);
        float delta = rmse - preRMSE;
        if (numberOfStatistics > 1 && delta > 0F) {
            return true;
        }
        preRMSE = rmse;
        return false;
    }

    private float predict(int userIndex, int itemIndex) {
        float value = 0F;
        for (Entry<Float, Integer> term : scoreIndexes.entrySet()) {
            float score = term.getKey();
            float probability = 0F;
            for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                probability += userTopicProbabilities.getValue(userIndex, topicIndex) * topicItemScoreProbabilities[topicIndex][itemIndex][term.getValue()];
            }
            value += probability * score;
        }
        return value;
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        instance.setQuantityMark(predict(userIndex, itemIndex));
    }

}
