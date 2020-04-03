package com.jstarcraft.rns.model.collaborative.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.common.configuration.Configurator;

/**
 * 
 * Asymmetric SVD++推荐器
 * 
 * <pre>
 * Factorization Meets the Neighborhood: a Multifaceted Collaborative Filtering Model
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class ASVDPlusPlusModel extends BiasedMFModel {

    private DenseMatrix positiveFactors, negativeFactors;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        positiveFactors = DenseMatrix.valueOf(itemSize, factorSize);
        positiveFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });
        negativeFactors = DenseMatrix.valueOf(itemSize, factorSize);
        negativeFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });
    }

    @Override
    protected void doPractice() {
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            // TODO 目前没有totalLoss.
            totalError = 0f;
            for (MatrixScalar matrixTerm : scoreMatrix) {
                int userIndex = matrixTerm.getRow();
                int itemIndex = matrixTerm.getColumn();
                float score = matrixTerm.getValue();
                float predict = predict(userIndex, itemIndex);
                float error = score - predict;
                SparseVector userVector = scoreMatrix.getRowVector(userIndex);

                // update factors
                float userBiasValue = userBiases.getValue(userIndex);
                userBiases.shiftValue(userIndex, learnRatio * (error - regBias * userBiasValue));
                float itemBiasValue = itemBiases.getValue(itemIndex);
                itemBiases.shiftValue(itemIndex, learnRatio * (error - regBias * itemBiasValue));

                float squareRoot = (float) Math.sqrt(userVector.getElementSize());
                float[] positiveSums = new float[factorSize];
                float[] negativeSums = new float[factorSize];
                for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                    float positiveSum = 0F;
                    float negativeSum = 0F;
                    for (VectorScalar term : userVector) {
                        int ItemIdx = term.getIndex();
                        positiveSum += positiveFactors.getValue(ItemIdx, factorIndex);
                        negativeSum += negativeFactors.getValue(ItemIdx, factorIndex) * (score - meanScore - userBiases.getValue(userIndex) - itemBiases.getValue(ItemIdx));
                    }
                    positiveSums[factorIndex] = squareRoot > 0 ? positiveSum / squareRoot : positiveSum;
                    negativeSums[factorIndex] = squareRoot > 0 ? negativeSum / squareRoot : negativeSum;
                }

                for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                    float userFactor = userFactors.getValue(userIndex, factorIndex);
                    float itemFactor = itemFactors.getValue(itemIndex, factorIndex);
                    float userValue = error * itemFactor - userRegularization * userFactor;
                    float itemValue = error * (userFactor + positiveSums[factorIndex] + negativeSums[factorIndex]) - itemRegularization * itemFactor;
                    userFactors.shiftValue(userIndex, factorIndex, learnRatio * userValue);
                    itemFactors.shiftValue(itemIndex, factorIndex, learnRatio * itemValue);
                    for (VectorScalar term : userVector) {
                        int index = term.getIndex();
                        float positiveFactor = positiveFactors.getValue(index, factorIndex);
                        float negativeFactor = negativeFactors.getValue(index, factorIndex);
                        float positiveDelta = error * itemFactor / squareRoot - userRegularization * positiveFactor;
                        float negativeDelta = error * itemFactor * (score - meanScore - userBiases.getValue(userIndex) - itemBiases.getValue(index)) / squareRoot - userRegularization * negativeFactor;
                        positiveFactors.shiftValue(index, factorIndex, learnRatio * positiveDelta);
                        negativeFactors.shiftValue(index, factorIndex, learnRatio * negativeDelta);
                    }
                }
            }
        }
    }

    @Override
    protected float predict(int userIndex, int itemIndex) {
        DefaultScalar scalar = DefaultScalar.getInstance();
        DenseVector userVector = userFactors.getRowVector(userIndex);
        DenseVector itemVector = itemFactors.getRowVector(itemIndex);
        float value = meanScore + userBiases.getValue(userIndex) + itemBiases.getValue(itemIndex) + scalar.dotProduct(userVector, itemVector).getValue();
        SparseVector rateVector = scoreMatrix.getRowVector(userIndex);
        float squareRoot = (float) Math.sqrt(rateVector.getElementSize());
        for (VectorScalar term : rateVector) {
            itemIndex = term.getIndex();
            DenseVector positiveVector = positiveFactors.getRowVector(itemIndex);
            DenseVector negativeVector = negativeFactors.getRowVector(itemIndex);
            value += scalar.dotProduct(positiveVector, itemVector).getValue() / squareRoot;
            float scale = term.getValue() - meanScore - userBiases.getValue(userIndex) - itemBiases.getValue(itemIndex);
            value += scalar.dotProduct(negativeVector, itemVector).getValue() * scale / squareRoot;
        }
        if (Float.isNaN(value)) {
            value = meanScore;
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