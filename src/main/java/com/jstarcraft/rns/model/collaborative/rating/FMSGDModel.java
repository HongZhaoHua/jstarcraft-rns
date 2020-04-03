package com.jstarcraft.rns.model.collaborative.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.common.configuration.Configurator;
import com.jstarcraft.rns.model.FactorizationMachineModel;

/**
 * 
 * FM SGD推荐器
 * 
 * <pre>
 * Factorization Machines via Stochastic Gradient Descent with Square Loss
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class FMSGDModel extends FactorizationMachineModel {

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
    }

    @Override
    protected void doPractice() {
        DefaultScalar scalar = DefaultScalar.getInstance();
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            totalError = 0F;
            for (DataInstance sample : marker) {
                // TODO 因为每次的data都是1,可以考虑避免重复构建featureVector.
                MathVector featureVector = getFeatureVector(sample);
                float score = sample.getQuantityMark();
                float predict = predict(scalar, featureVector);

                float error = predict - score;
                totalError += error * error;

                // global bias
                totalError += biasRegularization * globalBias * globalBias;

                // TODO 因为此处相当与迭代trainTensor的featureVector,所以hW0才会是1D.
                float hW0 = 1F;
                float bias = error * hW0 + biasRegularization * globalBias;

                // update w0
                globalBias += -learnRatio * bias;

                // 1-way interactions
                for (VectorScalar outerTerm : featureVector) {
                    int outerIndex = outerTerm.getIndex();
                    float oldWeight = weightVector.getValue(outerIndex);
                    float featureWeight = outerTerm.getValue();
                    float newWeight = error * featureWeight + weightRegularization * oldWeight;
                    weightVector.shiftValue(outerIndex, -learnRatio * newWeight);
                    totalError += weightRegularization * oldWeight * oldWeight;
                    // 2-way interactions
                    for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                        float oldValue = featureFactors.getValue(outerIndex, factorIndex);
                        float newValue = 0F;
                        for (VectorScalar innerTerm : featureVector) {
                            int innerIndex = innerTerm.getIndex();
                            if (innerIndex != outerIndex) {
                                newValue += featureWeight * featureFactors.getValue(innerIndex, factorIndex) * innerTerm.getValue();
                            }
                        }
                        newValue = error * newValue + factorRegularization * oldValue;
                        featureFactors.shiftValue(outerIndex, factorIndex, -learnRatio * newValue);
                        totalError += factorRegularization * oldValue * oldValue;
                    }
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