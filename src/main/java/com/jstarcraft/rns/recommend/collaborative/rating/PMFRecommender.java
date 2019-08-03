package com.jstarcraft.rns.recommend.collaborative.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.rns.recommend.MatrixFactorizationRecommender;

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
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            totalError = 0F;
            for (MatrixScalar term : scoreMatrix) {
                int userIndex = term.getRow(); // user
                int itemIndex = term.getColumn(); // item
                float rate = term.getValue();
                float predict = predict(userIndex, itemIndex);
                float error = rate - predict;
                totalError += error * error;

                // update factors
                for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
                    float userFactor = userFactors.getValue(userIndex, factorIndex), itemFactor = itemFactors.getValue(itemIndex, factorIndex);
                    userFactors.shiftValue(userIndex, factorIndex, learnRate * (error * itemFactor - userRegularization * userFactor));
                    itemFactors.shiftValue(itemIndex, factorIndex, learnRate * (error * userFactor - itemRegularization * itemFactor));
                    totalError += userRegularization * userFactor * userFactor + itemRegularization * itemFactor * itemFactor;
                }
            }

            totalError *= 0.5F;
            if (isConverged(epocheIndex) && isConverged) {
                break;
            }
            isLearned(epocheIndex);
            currentError = totalError;
        }
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        float value = super.predict(userIndex, itemIndex);
        if (value > maximumOfScore) {
            value = maximumOfScore;
        } else if (value < minimumOfScore) {
            value = minimumOfScore;
        }
        instance.setQuantityMark(value);
    }

}
