package com.jstarcraft.rns.recommend.extend.rating;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.rns.configure.Configurator;
import com.jstarcraft.rns.recommend.AbstractRecommender;
import com.jstarcraft.rns.recommend.exception.RecommendException;

/**
 * 
 * Personality Diagnosis推荐器
 * 
 * <pre>
 * A brief introduction to Personality Diagnosis
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class PersonalityDiagnosisRecommender extends AbstractRecommender {
    /**
     * Gaussian noise: 2.5 suggested in the paper
     */
    private float sigma;

    /**
     * prior probability
     */
    private float prior;

    private ArrayList<Float> values;

    /**
     * initialization
     *
     * @throws RecommendException if error occurs
     */
    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        prior = 1F / userSize;
        sigma = configuration.getFloat("recommender.PersonalityDiagnosis.sigma");
        values = new ArrayList<>(scoreIndexes.keySet());
    }

    @Override
    protected void doPractice() {
    }

    /**
     * predict a specific rating for user userIdx on item itemIdx.
     *
     * @param userIndex user index
     * @param itemIndex item index
     * @return predictive rating for user userIdx on item itemIdx
     * @throws RecommendException if error occurs
     */
    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        float[] probabilities = new float[scoreIndexes.size()];
        SparseVector itemVector = scoreMatrix.getColumnVector(itemIndex);
        SparseVector rightUserVector = scoreMatrix.getRowVector(userIndex);
        for (VectorScalar term : itemVector) {
            // other users who rated item j
            userIndex = term.getIndex();
            float rate = term.getValue();
            float probability = 1F;
            SparseVector leftUserVector = scoreMatrix.getRowVector(userIndex);
            int leftIndex = 0, rightIndex = 0, leftSize = leftUserVector.getElementSize(), rightSize = rightUserVector.getElementSize();
            if (leftSize != 0 && rightSize != 0) {
                Iterator<VectorScalar> leftIterator = leftUserVector.iterator();
                Iterator<VectorScalar> rightIterator = rightUserVector.iterator();
                VectorScalar leftTerm = leftIterator.next();
                VectorScalar rightTerm = rightIterator.next();
                // 判断两个有序数组中是否存在相同的数字
                while (leftIndex < leftSize && rightIndex < rightSize) {
                    if (leftTerm.getIndex() == rightTerm.getIndex()) {
                        probability *= gaussian(rightTerm.getValue(), leftTerm.getValue(), sigma);
                        if (leftIterator.hasNext()) {
                            leftTerm = leftIterator.next();
                        }
                        if (rightIterator.hasNext()) {
                            rightTerm = rightIterator.next();
                        }
                        leftIndex++;
                        rightIndex++;
                    } else if (leftTerm.getIndex() > rightTerm.getIndex()) {
                        if (rightIterator.hasNext()) {
                            rightTerm = rightIterator.next();
                        }
                        rightIndex++;
                    } else if (leftTerm.getIndex() < rightTerm.getIndex()) {
                        if (leftIterator.hasNext()) {
                            leftTerm = leftIterator.next();
                        }
                        leftIndex++;
                    }
                }
            }
            for (Entry<Float, Integer> entry : scoreIndexes.entrySet()) {
                probabilities[entry.getValue()] += gaussian(entry.getKey(), rate, sigma) * probability;
            }
        }
        for (Entry<Float, Integer> entry : scoreIndexes.entrySet()) {
            probabilities[entry.getValue()] *= prior;
        }
        int valueIndex = 0;
        float probability = Float.MIN_VALUE;
        for (int rateIndex = 0; rateIndex < probabilities.length; rateIndex++) {
            if (probabilities[rateIndex] > probability) {
                probability = probabilities[rateIndex];
                valueIndex = rateIndex;
            }
        }
        instance.setQuantityMark(values.get(valueIndex));
    }

    /**
     * 非标准高斯实现
     * 
     * @param value
     * @param mean
     * @param standardDeviation
     * @return
     */
    private static float gaussian(float value, float mean, float standardDeviation) {
        value = value - mean;
        value = value / standardDeviation;
        return (float) (Math.exp(-0.5F * value * value));
    }

}
