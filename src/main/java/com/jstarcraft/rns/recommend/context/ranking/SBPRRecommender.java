package com.jstarcraft.rns.recommend.context.ranking;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.configure.Configuration;
import com.jstarcraft.rns.recommend.SocialRecommender;
import com.jstarcraft.rns.utility.LogisticUtility;

import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * 
 * SBPR推荐器
 * 
 * <pre>
 * Social Bayesian Personalized Ranking (SBPR)
 * Leveraging Social Connections to Improve Personalized Ranking for Collaborative Filtering
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
// TODO 仍需重构
public class SBPRRecommender extends SocialRecommender {
    /**
     * items biases vector
     */
    private DenseVector itemBiases;

    /**
     * bias regularization
     */
    protected float regBias;

    /**
     * find items rated by trusted neighbors only
     */
    // TODO 考虑重构为List<IntSet>
    private List<List<Integer>> socialItemList;

    private List<IntSet> userItemSet;

    @Override
    public void prepare(Configuration configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        regBias = configuration.getFloat("recommender.bias.regularization", 0.01F);
        // cacheSpec = conf.get("guava.cache.spec",
        // "maximumSize=5000,expireAfterAccess=50m");

        itemBiases = DenseVector.valueOf(itemSize);
        itemBiases.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });

        userItemSet = getUserItemSet(scoreMatrix);

        // TODO 考虑重构
        // find items rated by trusted neighbors only
        socialItemList = new ArrayList<>(userSize);

        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            SparseVector userVector = scoreMatrix.getRowVector(userIndex);
            IntSet itemSet = userItemSet.get(userIndex);
            // find items rated by trusted neighbors only

            SparseVector socialVector = socialMatrix.getRowVector(userIndex);
            List<Integer> socialList = new LinkedList<>();
            for (VectorScalar term : socialVector) {
                int socialIndex = term.getIndex();
                userVector = scoreMatrix.getRowVector(socialIndex);
                for (VectorScalar enrty : userVector) {
                    int itemIndex = enrty.getIndex();
                    // v's rated items
                    if (!itemSet.contains(itemIndex) && !socialList.contains(itemIndex)) {
                        socialList.add(itemIndex);
                    }
                }
            }
            socialItemList.add(new ArrayList<>(socialList));
        }
    }

    @Override
    protected void doPractice() {
        for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
            totalLoss = 0F;
            for (int sampleIndex = 0, sampleTimes = userSize * 100; sampleIndex < sampleTimes; sampleIndex++) {
                // uniformly draw (userIdx, posItemIdx, k, negItemIdx)
                int userIndex, positiveItemIndex, negativeItemIndex;
                // userIdx
                SparseVector userVector;
                do {
                    userIndex = RandomUtility.randomInteger(userSize);
                    userVector = scoreMatrix.getRowVector(userIndex);
                } while (userVector.getElementSize() == 0);

                // positive item index
                positiveItemIndex = userVector.getIndex(RandomUtility.randomInteger(userVector.getElementSize()));
                float positiveRate = predict(userIndex, positiveItemIndex);

                // social Items List
                // TODO 应该修改为IntSet合适点.
                List<Integer> socialList = socialItemList.get(userIndex);
                IntSet itemSet = userItemSet.get(userIndex);
                do {
                    negativeItemIndex = RandomUtility.randomInteger(itemSize);
                } while (itemSet.contains(negativeItemIndex) || socialList.contains(negativeItemIndex));
                float negativeRate = predict(userIndex, negativeItemIndex);

                if (socialList.size() > 0) {
                    // if having social neighbors
                    int itemIndex = socialList.get(RandomUtility.randomInteger(socialList.size()));
                    float socialRate = predict(userIndex, itemIndex);
                    SparseVector socialVector = socialMatrix.getRowVector(userIndex);
                    float socialWeight = 0F;
                    for (VectorScalar term : socialVector) {
                        int socialIndex = term.getIndex();
                        itemSet = userItemSet.get(socialIndex);
                        if (itemSet.contains(itemIndex)) {
                            socialWeight += 1;
                        }
                    }
                    float positiveError = (positiveRate - socialRate) / (1 + socialWeight);
                    float negativeError = socialRate - negativeRate;
                    float positiveGradient = LogisticUtility.getValue(-positiveError), negativeGradient = LogisticUtility.getValue(-negativeError);
                    float error = (float) (-Math.log(1 - positiveGradient) - Math.log(1 - negativeGradient));
                    totalLoss += error;

                    // update bi, bk, bj
                    float positiveBias = itemBiases.getValue(positiveItemIndex);
                    itemBiases.shiftValue(positiveItemIndex, learnRate * (positiveGradient / (1F + socialWeight) - regBias * positiveBias));
                    totalLoss += regBias * positiveBias * positiveBias;
                    float socialBias = itemBiases.getValue(itemIndex);
                    itemBiases.shiftValue(itemIndex, learnRate * (-positiveGradient / (1F + socialWeight) + negativeGradient - regBias * socialBias));
                    totalLoss += regBias * socialBias * socialBias;
                    float negativeBias = itemBiases.getValue(negativeItemIndex);
                    itemBiases.shiftValue(negativeItemIndex, learnRate * (-negativeGradient - regBias * negativeBias));
                    totalLoss += regBias * negativeBias * negativeBias;

                    // update P, Q
                    for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
                        float userFactor = userFactors.getValue(userIndex, factorIndex);
                        float positiveFactor = itemFactors.getValue(positiveItemIndex, factorIndex);
                        float itemFactor = itemFactors.getValue(itemIndex, factorIndex);
                        float negativeFactor = itemFactors.getValue(negativeItemIndex, factorIndex);
                        float delta = positiveGradient * (positiveFactor - itemFactor) / (1F + socialWeight) + negativeGradient * (itemFactor - negativeFactor);
                        userFactors.shiftValue(userIndex, factorIndex, learnRate * (delta - userRegularization * userFactor));
                        itemFactors.shiftValue(positiveItemIndex, factorIndex, learnRate * (positiveGradient * userFactor / (1F + socialWeight) - itemRegularization * positiveFactor));
                        itemFactors.shiftValue(negativeItemIndex, factorIndex, learnRate * (negativeGradient * (-userFactor) - itemRegularization * negativeFactor));
                        delta = positiveGradient * (-userFactor / (1F + socialWeight)) + negativeGradient * userFactor;
                        itemFactors.shiftValue(itemIndex, factorIndex, learnRate * (delta - itemRegularization * itemFactor));
                        totalLoss += userRegularization * userFactor * userFactor + itemRegularization * positiveFactor * positiveFactor + itemRegularization * negativeFactor * negativeFactor + itemRegularization * itemFactor * itemFactor;
                    }
                } else {
                    // if no social neighbors, the same as BPR
                    float error = positiveRate - negativeRate;
                    totalLoss += error;
                    float gradient = LogisticUtility.getValue(-error);

                    // update bi, bj
                    float positiveBias = itemBiases.getValue(positiveItemIndex);
                    itemBiases.shiftValue(positiveItemIndex, learnRate * (gradient - regBias * positiveBias));
                    totalLoss += regBias * positiveBias * positiveBias;
                    float negativeBias = itemBiases.getValue(negativeItemIndex);
                    itemBiases.shiftValue(negativeItemIndex, learnRate * (-gradient - regBias * negativeBias));
                    totalLoss += regBias * negativeBias * negativeBias;

                    // update user factors, item factors
                    for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
                        float userFactor = userFactors.getValue(userIndex, factorIndex);
                        float positiveFactor = itemFactors.getValue(positiveItemIndex, factorIndex);
                        float negItemFactorValue = itemFactors.getValue(negativeItemIndex, factorIndex);
                        userFactors.shiftValue(userIndex, factorIndex, learnRate * (gradient * (positiveFactor - negItemFactorValue) - userRegularization * userFactor));
                        itemFactors.shiftValue(positiveItemIndex, factorIndex, learnRate * (gradient * userFactor - itemRegularization * positiveFactor));
                        itemFactors.shiftValue(negativeItemIndex, factorIndex, learnRate * (gradient * (-userFactor) - itemRegularization * negItemFactorValue));
                        totalLoss += userRegularization * userFactor * userFactor + itemRegularization * positiveFactor * positiveFactor + itemRegularization * negItemFactorValue * negItemFactorValue;
                    }
                }
            }

            if (isConverged(iterationStep) && isConverged) {
                break;
            }
            isLearned(iterationStep);
            currentLoss = totalLoss;
        }
    }

    @Override
    protected float predict(int userIndex, int itemIndex) {
        DefaultScalar scalar = DefaultScalar.getInstance();
        DenseVector userVector = userFactors.getRowVector(userIndex);
        DenseVector itemVector = itemFactors.getRowVector(itemIndex);
        return itemBiases.getValue(itemIndex) + scalar.dotProduct(userVector, itemVector).getValue();
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        instance.setQuantityMark(predict(userIndex, itemIndex));
    }

}
