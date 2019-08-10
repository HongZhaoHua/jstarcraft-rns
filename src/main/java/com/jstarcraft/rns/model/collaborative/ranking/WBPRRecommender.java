package com.jstarcraft.rns.model.collaborative.ranking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.model.MatrixFactorizationRecommender;
import com.jstarcraft.rns.utility.LogisticUtility;

import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * 
 * WBPR推荐器
 * 
 * <pre>
 * Bayesian Personalized Ranking for Non-Uniformly Sampled Items
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class WBPRRecommender extends MatrixFactorizationRecommender {
    /**
     * user items Set
     */
    // private LoadingCache<Integer, IntSet> userItemsSet;

    /**
     * pre-compute and sort by item's popularity
     */
    private List<KeyValue<Integer, Double>> itemPopularities;

    private List<KeyValue<Integer, Double>>[] itemProbabilities;

    /**
     * items biases
     */
    private DenseVector itemBiases;

    /**
     * bias regularization
     */
    private float biasRegularization;

    /**
     * Guava cache configuration
     */
    // protected static String cacheSpec;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        biasRegularization = configuration.getFloat("recommender.bias.regularization", 0.01F);

        itemBiases = DenseVector.valueOf(itemSize);
        itemBiases.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(0.01F));
        });

        // pre-compute and sort by item's popularity
        itemPopularities = new ArrayList<>(itemSize);
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            itemPopularities.add(new KeyValue<>(itemIndex, Double.valueOf(scoreMatrix.getColumnScope(itemIndex))));
        }
        Collections.sort(itemPopularities, (left, right) -> {
            // 降序
            return right.getValue().compareTo(left.getValue());
        });

        itemProbabilities = new List[userSize];
        List<IntSet> userItemSet = getUserItemSet(scoreMatrix);
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            IntSet scoreSet = userItemSet.get(userIndex);
            List<KeyValue<Integer, Double>> probabilities = new LinkedList<>();
            itemProbabilities[userIndex] = probabilities;
            // filter candidate items
            double sum = 0;
            for (KeyValue<Integer, Double> term : itemPopularities) {
                int itemIndex = term.getKey();
                double popularity = term.getValue();
                if (!scoreSet.contains(itemIndex) && popularity > 0D) {
                    // make a clone to prevent bugs from normalization
                    probabilities.add(term);
                    sum += popularity;
                }
            }
            // normalization
            for (KeyValue<Integer, Double> term : probabilities) {
                term.setValue(term.getValue() / sum);
            }
        }
    }

    @Override
    protected void doPractice() {
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            totalError = 0F;
            for (int sampleIndex = 0, sampleTimes = userSize * 100; sampleIndex < sampleTimes; sampleIndex++) {
                // randomly draw (userIdx, posItemIdx, negItemIdx)
                int userIndex, positiveItemIndex, negativeItemIndex = 0;
                List<KeyValue<Integer, Double>> probabilities;
                while (true) {
                    userIndex = RandomUtility.randomInteger(userSize);
                    SparseVector userVector = scoreMatrix.getRowVector(userIndex);
                    if (userVector.getElementSize() == 0) {
                        continue;
                    }
                    positiveItemIndex = userVector.getIndex(RandomUtility.randomInteger(userVector.getElementSize()));
                    // sample j by popularity (probability)
                    probabilities = itemProbabilities[userIndex];
                    double random = RandomUtility.randomDouble(1D);
                    for (KeyValue<Integer, Double> term : probabilities) {
                        if ((random -= term.getValue()) <= 0D) {
                            negativeItemIndex = term.getKey();
                            break;
                        }
                    }
                    break;
                }

                // update parameters
                float positiveScore = predict(userIndex, positiveItemIndex);
                float negativeScore = predict(userIndex, negativeItemIndex);
                float error = positiveScore - negativeScore;
                float value = (float) -Math.log(LogisticUtility.getValue(error));
                totalError += value;
                value = LogisticUtility.getValue(-error);

                // update bias
                float positiveBias = itemBiases.getValue(positiveItemIndex), negativeBias = itemBiases.getValue(negativeItemIndex);
                itemBiases.shiftValue(positiveItemIndex, learnRatio * (value - biasRegularization * positiveBias));
                itemBiases.shiftValue(negativeItemIndex, learnRatio * (-value - biasRegularization * negativeBias));
                totalError += biasRegularization * (positiveBias * positiveBias + negativeBias * negativeBias);

                // update user/item vectors
                for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                    float userFactor = userFactors.getValue(userIndex, factorIndex);
                    float positiveItemFactor = itemFactors.getValue(positiveItemIndex, factorIndex);
                    float negativeItemFactor = itemFactors.getValue(negativeItemIndex, factorIndex);
                    userFactors.shiftValue(userIndex, factorIndex, learnRatio * (value * (positiveItemFactor - negativeItemFactor) - userRegularization * userFactor));
                    itemFactors.shiftValue(positiveItemIndex, factorIndex, learnRatio * (value * userFactor - itemRegularization * positiveItemFactor));
                    itemFactors.shiftValue(negativeItemIndex, factorIndex, learnRatio * (value * (-userFactor) - itemRegularization * negativeItemFactor));
                    totalError += userRegularization * userFactor * userFactor + itemRegularization * positiveItemFactor * positiveItemFactor + itemRegularization * negativeItemFactor * negativeItemFactor;
                }
            }
            if (isConverged(epocheIndex) && isConverged) {
                break;
            }
            isLearned(epocheIndex);
            currentError = totalError;
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
