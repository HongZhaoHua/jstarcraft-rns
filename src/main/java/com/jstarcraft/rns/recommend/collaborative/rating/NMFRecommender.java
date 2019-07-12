package com.jstarcraft.rns.recommend.collaborative.rating;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.ArrayVector;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.utility.MathUtility;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.configurator.Configuration;
import com.jstarcraft.rns.recommend.MatrixFactorizationRecommender;

/**
 * 
 * NMF推荐器
 * 
 * <pre>
 * Algorithms for Non-negative Matrix Factorization
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class NMFRecommender extends MatrixFactorizationRecommender {

    @Override
    public void prepare(Configuration configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        userFactors = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
        userFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(0.01F));
        });
        itemFactors = DenseMatrix.valueOf(numberOfItems, numberOfFactors);
        itemFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(0.01F));
        });
    }

    @Override
    protected void doPractice() {
        DefaultScalar scalar = DefaultScalar.getInstance();
        for (int iterationStep = 1; iterationStep <= numberOfEpoches; ++iterationStep) {
            // update userFactors by fixing itemFactors
            for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
                SparseVector userVector = scoreMatrix.getRowVector(userIndex);
                if (userVector.getElementSize() == 0) {
                    continue;
                }
                int user = userIndex;
                ArrayVector predictVector = new ArrayVector(userVector);
                predictVector.iterateElement(MathCalculator.SERIAL, (element) -> {
                    element.setValue(predict(user, element.getIndex()));
                });
                for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
                    DenseVector factorVector = itemFactors.getColumnVector(factorIndex);
                    float rate = scalar.dotProduct(factorVector, userVector).getValue();
                    float predict = scalar.dotProduct(factorVector, predictVector).getValue() + MathUtility.EPSILON;
                    userFactors.setValue(userIndex, factorIndex, userFactors.getValue(userIndex, factorIndex) * (rate / predict));
                }
            }

            // update itemFactors by fixing userFactors
            for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
                SparseVector itemVector = scoreMatrix.getColumnVector(itemIndex);
                if (itemVector.getElementSize() == 0) {
                    continue;
                }
                int item = itemIndex;
                ArrayVector predictVector = new ArrayVector(itemVector);
                predictVector.iterateElement(MathCalculator.SERIAL, (element) -> {
                    element.setValue(predict(element.getIndex(), item));
                });
                for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
                    DenseVector factorVector = userFactors.getColumnVector(factorIndex);
                    float rate = scalar.dotProduct(factorVector, itemVector).getValue();
                    float predict = scalar.dotProduct(factorVector, predictVector).getValue() + MathUtility.EPSILON;
                    itemFactors.setValue(itemIndex, factorIndex, itemFactors.getValue(itemIndex, factorIndex) * (rate / predict));
                }
            }

            // compute errors
            totalLoss = 0F;
            for (MatrixScalar term : scoreMatrix) {
                int userIndex = term.getRow();
                int itemIndex = term.getColumn();
                float rate = term.getValue();
                if (rate > 0) {
                    float error = predict(userIndex, itemIndex) - rate;
                    totalLoss += error * error;
                }
            }
            totalLoss *= 0.5F;
            if (isConverged(iterationStep) && isConverged) {
                break;
            }
            currentLoss = totalLoss;
        }
    }

}
