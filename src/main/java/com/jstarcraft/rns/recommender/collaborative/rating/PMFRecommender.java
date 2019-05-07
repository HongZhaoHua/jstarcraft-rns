package com.jstarcraft.rns.recommender.collaborative.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.rns.recommender.MatrixFactorizationRecommender;

/**
 * 
 * PMF推荐器
 * 
 * <pre>
 * PMF: Probabilistic Matrix Factorization
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class PMFRecommender extends MatrixFactorizationRecommender {

    @Override
    protected void doPractice() {
        for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
            totalLoss = 0F;
            for (MatrixScalar term : trainMatrix) {
                int userIndex = term.getRow(); // user
                int itemIndex = term.getColumn(); // item
                float rate = term.getValue();
                float predict = predict(userIndex, itemIndex);
                float error = rate - predict;
                totalLoss += error * error;

                // update factors
                for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
                    float userFactor = userFactors.getValue(userIndex, factorIndex), itemFactor = itemFactors.getValue(itemIndex, factorIndex);
                    userFactors.shiftValue(userIndex, factorIndex, learnRate * (error * itemFactor - userRegularization * userFactor));
                    itemFactors.shiftValue(itemIndex, factorIndex, learnRate * (error * userFactor - itemRegularization * itemFactor));
                    totalLoss += userRegularization * userFactor * userFactor + itemRegularization * itemFactor * itemFactor;
                }
            }

            totalLoss *= 0.5F;
            if (isConverged(iterationStep) && isConverged) {
                break;
            }
            isLearned(iterationStep);
            currentLoss = totalLoss;
        }
    }

    @Override
    public float predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        float value = super.predict(userIndex, itemIndex);
        if (value > maximumOfScore) {
            value = maximumOfScore;
        } else if (value < minimumOfScore) {
            value = minimumOfScore;
        }
        return value;
    }

}
