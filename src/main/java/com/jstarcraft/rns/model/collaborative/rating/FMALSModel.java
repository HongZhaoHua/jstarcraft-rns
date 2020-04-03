package com.jstarcraft.rns.model.collaborative.rating;

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
import com.jstarcraft.core.common.configuration.Configurator;
import com.jstarcraft.rns.model.FactorizationMachineModel;

import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;

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
public class FMALSModel extends FactorizationMachineModel {

    /**
     * train appender matrix
     */
    private SparseMatrix featureMatrix;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        // init Q
        // TODO 此处为rateFactors
        actionFactors = DenseMatrix.valueOf(actionSize, factorSize);

        // construct training appender matrix
        HashMatrix table = new HashMatrix(true, actionSize, featureSize, new Long2FloatRBTreeMap());
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
        featureMatrix = SparseMatrix.valueOf(actionSize, featureSize, table);
    }

    @Override
    protected void doPractice() {
        DefaultScalar scalar = DefaultScalar.getInstance();
        // precomputing Q and errors, for efficiency
        DenseVector errorVector = DenseVector.valueOf(actionSize);
        int index = 0;
        for (DataInstance sample : marker) {
            // TODO 因为每次的data都是1,可以考虑避免重复构建featureVector.
            MathVector featureVector = getFeatureVector(sample);
            float score = sample.getQuantityMark();
            float predict = predict(scalar, featureVector);

            float error = score - predict;
            errorVector.setValue(index, error);

            for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
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
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            totalError = 0F;
            // global bias
            float numerator = 0F;
            float denominator = 0F;

            for (int scoreIndex = 0; scoreIndex < actionSize; scoreIndex++) {
                // TODO 因为此处相当与迭代trainTensor的featureVector,所以h_theta才会是1D.
                float h_theta = 1F;
                numerator += globalBias * h_theta * h_theta + h_theta * errorVector.getValue(scoreIndex);
                denominator += h_theta;
            }
            denominator += biasRegularization;
            float bias = numerator / denominator;
            // update errors
            for (int scoreIndex = 0; scoreIndex < actionSize; scoreIndex++) {
                float oldError = errorVector.getValue(scoreIndex);
                float newError = oldError + (globalBias - bias);
                errorVector.setValue(scoreIndex, newError);
                totalError += oldError * oldError;
            }

            // update w0
            globalBias = bias;
            totalError += biasRegularization * globalBias * globalBias;

            // 1-way interactions
            for (int featureIndex = 0; featureIndex < featureSize; featureIndex++) {
                float oldWeight = weightVector.getValue(featureIndex);
                numerator = 0F;
                denominator = 0F;
                // TODO 考虑重构
                SparseVector featureVector = featureMatrix.getColumnVector(featureIndex);
                for (VectorScalar vectorTerm : featureVector) {
                    int scoreIndex = vectorTerm.getIndex();
                    float h_theta = vectorTerm.getValue();
                    numerator += oldWeight * h_theta * h_theta + h_theta * errorVector.getValue(scoreIndex);
                    denominator += h_theta * h_theta;
                }
                denominator += weightRegularization;
                float newWeight = numerator / denominator;
                // update errors
                for (VectorScalar vectorTerm : featureVector) {
                    int scoreIndex = vectorTerm.getIndex();
                    float oldError = errorVector.getValue(scoreIndex);
                    float newError = oldError + (oldWeight - newWeight) * vectorTerm.getValue();
                    errorVector.setValue(scoreIndex, newError);
                }
                // update W
                weightVector.setValue(featureIndex, newWeight);
                totalError += weightRegularization * oldWeight * oldWeight;
            }

            // 2-way interactions
            for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                for (int featureIndex = 0; featureIndex < featureSize; featureIndex++) {
                    float oldValue = featureFactors.getValue(featureIndex, factorIndex);
                    numerator = 0F;
                    denominator = 0F;
                    SparseVector featureVector = featureMatrix.getColumnVector(featureIndex);
                    for (VectorScalar vectorTerm : featureVector) {
                        int scoreIndex = vectorTerm.getIndex();
                        float x_val = vectorTerm.getValue();
                        float h_theta = x_val * (actionFactors.getValue(scoreIndex, factorIndex) - oldValue * x_val);
                        numerator += oldValue * h_theta * h_theta + h_theta * errorVector.getValue(scoreIndex);
                        denominator += h_theta * h_theta;
                    }
                    denominator += factorRegularization;
                    float newValue = numerator / denominator;
                    // update errors and Q
                    for (VectorScalar vectorTerm : featureVector) {
                        int scoreIndex = vectorTerm.getIndex();
                        float x_val = vectorTerm.getValue();
                        float oldScore = actionFactors.getValue(scoreIndex, factorIndex);
                        float newScore = oldScore + (newValue - oldValue) * x_val;
                        float h_theta_old = x_val * (oldScore - oldValue * x_val);
                        float h_theta_new = x_val * (newScore - newValue * x_val);
                        float oldError = errorVector.getValue(scoreIndex);
                        float newError = oldError + oldValue * h_theta_old - newValue * h_theta_new;
                        errorVector.setValue(scoreIndex, newError);
                        actionFactors.setValue(scoreIndex, factorIndex, newScore);
                    }

                    // update V
                    featureFactors.setValue(featureIndex, factorIndex, newValue);
                    totalError += factorRegularization * oldValue * oldValue;
                }
            }

            if (isConverged(epocheIndex) && isConverged) {
                break;
            }
            currentError = totalError;
        }
    }

}