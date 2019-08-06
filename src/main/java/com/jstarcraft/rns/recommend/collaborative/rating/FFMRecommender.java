package com.jstarcraft.rns.recommend.collaborative.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.rns.configure.Configurator;
import com.jstarcraft.rns.recommend.FactorizationMachineRecommender;

/**
 * 
 * FFM推荐器
 * 
 * <pre>
 * Field Aware Factorization Machines for CTR Prediction
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class FFMRecommender extends FactorizationMachineRecommender {

    /**
     * record the <feature: filed>
     */
    private int[] featureOrders;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);

        // Matrix for p * (factor * filed)
        // TODO 此处应该还是稀疏
        featureFactors = DenseMatrix.valueOf(featureSize, factorSize * marker.getQualityOrder());
        featureFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });

        // init the map for feature of filed
        featureOrders = new int[featureSize];
        int count = 0;
        for (int orderIndex = 0, orderSize = dimensionSizes.length; orderIndex < orderSize; orderIndex++) {
            int size = dimensionSizes[orderIndex];
            for (int index = 0; index < size; index++) {
                featureOrders[count + index] = orderIndex;
            }
            count += size;
        }
    }

    @Override
    protected void doPractice() {
        DefaultScalar scalar = DefaultScalar.getInstance();
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            totalError = 0F;
            int outerIndex = 0;
            int innerIndex = 0;
            float outerValue = 0F;
            float innerValue = 0F;
            float oldWeight = 0F;
            float newWeight = 0F;
            float oldFactor = 0F;
            float newFactor = 0F;
            for (DataInstance sample : marker) {
                // TODO 因为每次的data都是1,可以考虑避免重复构建featureVector.
                MathVector featureVector = getFeatureVector(sample);
                float score = sample.getQuantityMark();
                float predict = predict(scalar, featureVector);
                float error = predict - score;
                totalError += error * error;

                // global bias
                totalError += biasRegularization * globalBias * globalBias;

                // update w0
                float hW0 = 1;
                float gradW0 = error * hW0 + biasRegularization * globalBias;
                globalBias += -learnRatio * gradW0;

                // 1-way interactions
                for (VectorScalar outerTerm : featureVector) {
                    outerIndex = outerTerm.getIndex();
                    innerIndex = 0;
                    oldWeight = weightVector.getValue(outerIndex);
                    newWeight = outerTerm.getValue();
                    newWeight = error * newWeight + weightRegularization * oldWeight;
                    weightVector.shiftValue(outerIndex, -learnRatio * newWeight);
                    totalError += weightRegularization * oldWeight * oldWeight;
                    outerValue = outerTerm.getValue();
                    innerValue = 0F;
                    // 2-way interactions
                    for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                        oldFactor = featureFactors.getValue(outerIndex, featureOrders[outerIndex] + factorIndex);
                        newFactor = 0F;
                        for (VectorScalar innerTerm : featureVector) {
                            innerIndex = innerTerm.getIndex();
                            innerValue = innerTerm.getValue();
                            if (innerIndex != outerIndex) {
                                newFactor += outerValue * featureFactors.getValue(innerIndex, featureOrders[outerIndex] + factorIndex) * innerValue;
                            }
                        }
                        newFactor = error * newFactor + factorRegularization * oldFactor;
                        featureFactors.shiftValue(outerIndex, featureOrders[outerIndex] + factorIndex, -learnRatio * newFactor);
                        totalError += factorRegularization * oldFactor * oldFactor;
                    }
                }
            }

            totalError *= 0.5;
            if (isConverged(epocheIndex) && isConverged) {
                break;
            }
            currentError = totalError;
        }
    }

    @Override
    protected float predict(DefaultScalar scalar, MathVector featureVector) {
        float value = 0F;
        // global bias
        value += globalBias;
        // 1-way interaction
        value += scalar.dotProduct(weightVector, featureVector).getValue();
        int outerIndex = 0;
        int innerIndex = 0;
        float outerValue = 0F;
        float innerValue = 0F;
        // 2-way interaction
        for (int featureIndex = 0; featureIndex < factorSize; featureIndex++) {
            for (VectorScalar outerVector : featureVector) {
                outerIndex = outerVector.getIndex();
                outerValue = outerVector.getValue();
                for (VectorScalar innerVector : featureVector) {
                    innerIndex = innerVector.getIndex();
                    innerValue = innerVector.getValue();
                    if (outerIndex != innerIndex) {
                        value += featureFactors.getValue(outerIndex, featureOrders[innerIndex] + featureIndex) * featureFactors.getValue(innerIndex, featureOrders[outerIndex] + featureIndex) * outerValue * innerValue;
                    }
                }
            }
        }
        return value;
    }

}
