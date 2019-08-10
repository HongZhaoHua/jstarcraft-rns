package com.jstarcraft.rns.model.collaborative.rating;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.MathUtility;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.ArrayVector;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.model.MatrixFactorizationModel;

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
public class NMFModel extends MatrixFactorizationModel {

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        userFactors = DenseMatrix.valueOf(userSize, factorSize);
        userFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(0.01F));
        });
        itemFactors = DenseMatrix.valueOf(itemSize, factorSize);
        itemFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(0.01F));
        });
    }

    @Override
    protected void doPractice() {
        DefaultScalar scalar = DefaultScalar.getInstance();
        for (int epocheIndex = 0; epocheIndex < epocheSize; ++epocheIndex) {
            // update userFactors by fixing itemFactors
            for (int userIndex = 0; userIndex < userSize; userIndex++) {
                SparseVector userVector = scoreMatrix.getRowVector(userIndex);
                if (userVector.getElementSize() == 0) {
                    continue;
                }
                int user = userIndex;
                ArrayVector predictVector = new ArrayVector(userVector);
                predictVector.iterateElement(MathCalculator.SERIAL, (element) -> {
                    element.setValue(predict(user, element.getIndex()));
                });
                for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                    DenseVector factorVector = itemFactors.getColumnVector(factorIndex);
                    float score = scalar.dotProduct(factorVector, userVector).getValue();
                    float predict = scalar.dotProduct(factorVector, predictVector).getValue() + MathUtility.EPSILON;
                    userFactors.setValue(userIndex, factorIndex, userFactors.getValue(userIndex, factorIndex) * (score / predict));
                }
            }

            // update itemFactors by fixing userFactors
            for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                SparseVector itemVector = scoreMatrix.getColumnVector(itemIndex);
                if (itemVector.getElementSize() == 0) {
                    continue;
                }
                int item = itemIndex;
                ArrayVector predictVector = new ArrayVector(itemVector);
                predictVector.iterateElement(MathCalculator.SERIAL, (element) -> {
                    element.setValue(predict(element.getIndex(), item));
                });
                for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                    DenseVector factorVector = userFactors.getColumnVector(factorIndex);
                    float score = scalar.dotProduct(factorVector, itemVector).getValue();
                    float predict = scalar.dotProduct(factorVector, predictVector).getValue() + MathUtility.EPSILON;
                    itemFactors.setValue(itemIndex, factorIndex, itemFactors.getValue(itemIndex, factorIndex) * (score / predict));
                }
            }

            // compute errors
            totalError = 0F;
            for (MatrixScalar term : scoreMatrix) {
                int userIndex = term.getRow();
                int itemIndex = term.getColumn();
                float score = term.getValue();
                if (score > 0) {
                    float error = predict(userIndex, itemIndex) - score;
                    totalError += error * error;
                }
            }
            totalError *= 0.5F;
            if (isConverged(epocheIndex) && isConverged) {
                break;
            }
            currentError = totalError;
        }
    }

}
