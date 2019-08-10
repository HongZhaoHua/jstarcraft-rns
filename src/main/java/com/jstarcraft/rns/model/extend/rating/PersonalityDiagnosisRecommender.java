package com.jstarcraft.rns.model.extend.rating;

import java.util.Iterator;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.rns.model.AbstractRecommender;
import com.jstarcraft.rns.model.exception.RecommendException;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.floats.FloatRBTreeSet;
import it.unimi.dsi.fastutil.floats.FloatSet;

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

    private FloatList scores;

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

        FloatSet sorts = new FloatRBTreeSet();
        for (MatrixScalar term : scoreMatrix) {
            sorts.add(term.getValue());
        }
        sorts.remove(0F);
        scores = new FloatArrayList(sorts);
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
        int scoreSize = scores.size();
        float[] probabilities = new float[scoreSize];
        SparseVector itemVector = scoreMatrix.getColumnVector(itemIndex);
        SparseVector rightUserVector = scoreMatrix.getRowVector(userIndex);
        for (VectorScalar term : itemVector) {
            // other users who rated item j
            userIndex = term.getIndex();
            float score = term.getValue();
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
            for (int scoreIndex = 0; scoreIndex < scoreSize; scoreIndex++) {
                probabilities[scoreIndex] += gaussian(scores.getFloat(scoreIndex), score, sigma) * probability;
            }
        }
        for (int scoreIndex = 0; scoreIndex < scoreSize; scoreIndex++) {
            probabilities[scoreIndex] *= prior;
        }
        int valueIndex = 0;
        float probability = Float.MIN_VALUE;
        for (int scoreIndex = 0; scoreIndex < probabilities.length; scoreIndex++) {
            if (probabilities[scoreIndex] > probability) {
                probability = probabilities[scoreIndex];
                valueIndex = scoreIndex;
            }
        }
        instance.setQuantityMark(scores.get(valueIndex));
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
