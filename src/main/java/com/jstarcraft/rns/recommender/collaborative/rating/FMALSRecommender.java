package com.jstarcraft.rns.recommender.collaborative.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.HashMatrix;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.rns.configurator.Configuration;
import com.jstarcraft.rns.recommender.FactorizationMachineRecommender;

import it.unimi.dsi.fastutil.ints.Int2FloatRBTreeMap;

/**
 * 
 * FM ALS推荐器
 * 
 * <pre>
 * Factorization Machines via Alternating Least Square
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class FMALSRecommender extends FactorizationMachineRecommender {

    /**
     * train appender matrix
     */
    private SparseMatrix featureMatrix;

    @Override
    public void prepare(Configuration configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        // init Q
        // TODO 此处为rateFactors
        actionFactors = DenseMatrix.valueOf(numberOfActions, numberOfFactors);

        // construct training appender matrix
        HashMatrix table = new HashMatrix(true, numberOfActions, numberOfFeatures, new Int2FloatRBTreeMap());
        int index = 0;
        int order = marker.getQualityOrder();
        for (DataInstance sample : model) {
            int count = 0;
            for (int orderIndex = 0; orderIndex < order; orderIndex++) {
                table.setValue(index, count + sample.getQualityFeature(orderIndex), 1F);
                count += dimensionSizes[orderIndex];
            }
            index++;
        }
        // TODO 考虑重构(.此处似乎就是FactorizationMachineRecommender.getFeatureVector);
        featureMatrix = SparseMatrix.valueOf(numberOfActions, numberOfFeatures, table);
    }

    @Override
    protected void doPractice() {
        DefaultScalar scalar = DefaultScalar.getInstance();
        // precomputing Q and errors, for efficiency
        DenseVector errorVector = DenseVector.valueOf(numberOfActions);
        int index = 0;
        for (DataInstance sample : marker) {
            // TODO 因为每次的data都是1,可以考虑避免重复构建featureVector.
            MathVector featureVector = getFeatureVector(sample);
            float rate = sample.getQuantityMark();
            float predict = predict(scalar, featureVector);

            float error = rate - predict;
            errorVector.setValue(index, error);

            for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
                float sum = 0F;
                for (VectorScalar vectorTerm : featureVector) {
                    sum += featureFactors.getValue(vectorTerm.getIndex(), factorIndex) * vectorTerm.getValue();
                }
                actionFactors.setValue(index, factorIndex, sum);
            }
            index++;
        }

        /**
         * parameter optimized by using formula in [1]. errors updated by using formula:
         * error_new = error_old + theta_old*h_old - theta_new * h_new; reference: [1].
         * Rendle, Steffen, "Factorization Machines with libFM." ACM Transactions on
         * Intelligent Systems and Technology, 2012.
         */
        for (int iterationStep = 0; iterationStep < numberOfEpoches; iterationStep++) {
            totalLoss = 0F;
            // global bias
            float numerator = 0F;
            float denominator = 0F;

            for (int rateIndex = 0; rateIndex < numberOfActions; rateIndex++) {
                // TODO 因为此处相当与迭代trainTensor的featureVector,所以h_theta才会是1D.
                float h_theta = 1F;
                numerator += globalBias * h_theta * h_theta + h_theta * errorVector.getValue(rateIndex);
                denominator += h_theta;
            }
            denominator += biasRegularization;
            float bias = numerator / denominator;
            // update errors
            for (int rateIndex = 0; rateIndex < numberOfActions; rateIndex++) {
                float oldError = errorVector.getValue(rateIndex);
                float newError = oldError + (globalBias - bias);
                errorVector.setValue(rateIndex, newError);
                totalLoss += oldError * oldError;
            }

            // update w0
            globalBias = bias;
            totalLoss += biasRegularization * globalBias * globalBias;

            // 1-way interactions
            for (int featureIndex = 0; featureIndex < numberOfFeatures; featureIndex++) {
                float oldWeight = weightVector.getValue(featureIndex);
                numerator = 0F;
                denominator = 0F;
                // TODO 考虑重构
                SparseVector featureVector = featureMatrix.getColumnVector(featureIndex);
                for (VectorScalar vectorTerm : featureVector) {
                    int rateIndex = vectorTerm.getIndex();
                    float h_theta = vectorTerm.getValue();
                    numerator += oldWeight * h_theta * h_theta + h_theta * errorVector.getValue(rateIndex);
                    denominator += h_theta * h_theta;
                }
                denominator += weightRegularization;
                float newWeight = numerator / denominator;
                // update errors
                for (VectorScalar vectorTerm : featureVector) {
                    int rateIndex = vectorTerm.getIndex();
                    float oldError = errorVector.getValue(rateIndex);
                    float newError = oldError + (oldWeight - newWeight) * vectorTerm.getValue();
                    errorVector.setValue(rateIndex, newError);
                }
                // update W
                weightVector.setValue(featureIndex, newWeight);
                totalLoss += weightRegularization * oldWeight * oldWeight;
            }

            // 2-way interactions
            for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
                for (int featureIndex = 0; featureIndex < numberOfFeatures; featureIndex++) {
                    float oldValue = featureFactors.getValue(featureIndex, factorIndex);
                    numerator = 0F;
                    denominator = 0F;
                    SparseVector featureVector = featureMatrix.getColumnVector(featureIndex);
                    for (VectorScalar vectorTerm : featureVector) {
                        int rateIndex = vectorTerm.getIndex();
                        float x_val = vectorTerm.getValue();
                        float h_theta = x_val * (actionFactors.getValue(rateIndex, factorIndex) - oldValue * x_val);
                        numerator += oldValue * h_theta * h_theta + h_theta * errorVector.getValue(rateIndex);
                        denominator += h_theta * h_theta;
                    }
                    denominator += factorRegularization;
                    float newValue = numerator / denominator;
                    // update errors and Q
                    for (VectorScalar vectorTerm : featureVector) {
                        int rateIndex = vectorTerm.getIndex();
                        float x_val = vectorTerm.getValue();
                        float oldRate = actionFactors.getValue(rateIndex, factorIndex);
                        float newRate = oldRate + (newValue - oldValue) * x_val;
                        float h_theta_old = x_val * (oldRate - oldValue * x_val);
                        float h_theta_new = x_val * (newRate - newValue * x_val);
                        float oldError = errorVector.getValue(rateIndex);
                        float newError = oldError + oldValue * h_theta_old - newValue * h_theta_new;
                        errorVector.setValue(rateIndex, newError);
                        actionFactors.setValue(rateIndex, factorIndex, newRate);
                    }

                    // update V
                    featureFactors.setValue(featureIndex, factorIndex, newValue);
                    totalLoss += factorRegularization * oldValue * oldValue;
                }
            }

            if (isConverged(iterationStep) && isConverged) {
                break;
            }
            currentLoss = totalLoss;
        }
    }

    @Override
    protected float predict(DefaultScalar scalar, MathVector featureVector) {
        float value = super.predict(scalar, featureVector);

        if (value > maximumOfScore) {
            value = maximumOfScore;
        }
        if (value < minimumOfScore) {
            value = minimumOfScore;
        }
        return value;
    }

}