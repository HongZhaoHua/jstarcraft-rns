package com.jstarcraft.rns.model.collaborative.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.core.common.option.Option;
import com.jstarcraft.rns.model.MatrixFactorizationModel;

/**
 * 
 * BiasedMF推荐器
 * 
 * <pre>
 * Biased Matrix Factorization
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class BiasedMFModel extends MatrixFactorizationModel {
    /**
     * bias regularization
     */
    protected float regBias;

    /**
     * user biases
     */
    protected DenseVector userBiases;

    /**
     * user biases
     */
    protected DenseVector itemBiases;

    @Override
    public void prepare(Option configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        regBias = configuration.getFloat("recommender.bias.regularization", 0.01F);

        // initialize the userBiased and itemBiased
        userBiases = DenseVector.valueOf(userSize);
        userBiases.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });
        itemBiases = DenseVector.valueOf(itemSize);
        itemBiases.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });
    }

    @Override
    protected void doPractice() {
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            totalError = 0F;

            for (MatrixScalar term : scoreMatrix) {
                int userIndex = term.getRow(); // user userIdx
                int itemIndex = term.getColumn(); // item itemIdx
                float score = term.getValue(); // real rating on item
                                               // itemIdx rated by user
                                               // userIdx
                float predict = predict(userIndex, itemIndex);
                float error = score - predict;
                totalError += error * error;

                // update user and item bias
                float userBias = userBiases.getValue(userIndex);
                userBiases.shiftValue(userIndex, learnRatio * (error - regBias * userBias));
                totalError += regBias * userBias * userBias;
                float itemBias = itemBiases.getValue(itemIndex);
                itemBiases.shiftValue(itemIndex, learnRatio * (error - regBias * itemBias));
                totalError += regBias * itemBias * itemBias;

                // update user and item factors
                for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                    float userFactor = userFactors.getValue(userIndex, factorIndex);
                    float itemFactor = itemFactors.getValue(itemIndex, factorIndex);
                    userFactors.shiftValue(userIndex, factorIndex, learnRatio * (error * itemFactor - userRegularization * userFactor));
                    itemFactors.shiftValue(itemIndex, factorIndex, learnRatio * (error * userFactor - itemRegularization * itemFactor));
                    totalError += userRegularization * userFactor * userFactor + itemRegularization * itemFactor * itemFactor;
                }
            }

            totalError *= 0.5D;
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
        float value = scalar.dotProduct(userVector, itemVector).getValue();
        value += meanScore + userBiases.getValue(userIndex) + itemBiases.getValue(itemIndex);
        return value;
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        instance.setQuantityMark(predict(userIndex, itemIndex));
    }

}
