package com.jstarcraft.rns.model.context.rating;

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
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.rns.model.SocialModel;
import com.jstarcraft.rns.model.exception.ModelException;

/**
 * 
 * TrustSVD推荐器
 * 
 * <pre>
 * TrustSVD: Collaborative Filtering with Both the Explicit and Implicit Influence of User Trust and of Item Ratings
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class TrustSVDModel extends SocialModel {

    private DenseMatrix itemExplicitFactors;

    /**
     * impitemExplicitFactors denotes the implicit influence of items rated by user
     * u in the past on the ratings of unknown items in the future.
     */
    private DenseMatrix itemImplicitFactors;

    private DenseMatrix trusterFactors;

    /**
     * the user-specific latent appender vector of users (trustees)trusted by user u
     */
    private DenseMatrix trusteeFactors;

    /**
     * weights of users(trustees) trusted by user u
     */
    private DenseVector trusteeWeights;

    /**
     * weights of users(trusters) who trust user u
     */
    private DenseVector trusterWeights;

    /**
     * weights of items rated by user u
     */
    private DenseVector itemWeights;

    /**
     * user biases and item biases
     */
    private DenseVector userBiases, itemBiases;

    /**
     * bias regularization
     */
    private float regBias;

    /**
     * initial the model
     *
     * @throws ModelException if error occurs
     */
    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        // trusterFactors.init(1.0);
        // itemExplicitFactors.init(1.0);
        regBias = configuration.getFloat("recommender.bias.regularization", 0.01F);

        // initialize userBiases and itemBiases
        // TODO 考虑重构
        userBiases = DenseVector.valueOf(userSize);
        userBiases.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });
        itemBiases = DenseVector.valueOf(itemSize);
        itemBiases.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });

        // initialize trusteeFactors and impitemExplicitFactors
        trusterFactors = userFactors;
        trusteeFactors = DenseMatrix.valueOf(userSize, factorSize);
        trusteeFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });
        itemExplicitFactors = itemFactors;
        itemImplicitFactors = DenseMatrix.valueOf(itemSize, factorSize);
        itemImplicitFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });

        // initialize trusteeWeights, trusterWeights, impItemWeights
        // TODO 考虑重构
        trusteeWeights = DenseVector.valueOf(userSize);
        trusterWeights = DenseVector.valueOf(userSize);
        itemWeights = DenseVector.valueOf(itemSize);
        int socialCount;
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            socialCount = socialMatrix.getColumnScope(userIndex);
            trusteeWeights.setValue(userIndex, (float) (socialCount > 0 ? 1F / Math.sqrt(socialCount) : 1F));
            socialCount = socialMatrix.getRowScope(userIndex);
            trusterWeights.setValue(userIndex, (float) (socialCount > 0 ? 1F / Math.sqrt(socialCount) : 1F));
        }
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            int count = scoreMatrix.getColumnScope(itemIndex);
            itemWeights.setValue(itemIndex, (float) (count > 0 ? 1F / Math.sqrt(count) : 1F));
        }
    }

    /**
     * train model process
     *
     * @throws ModelException if error occurs
     */
    @Override
    protected void doPractice() {
        DefaultScalar scalar = DefaultScalar.getInstance();
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            totalError = 0F;
            // temp user Factors and trustee factors
            DenseMatrix trusterDeltas = DenseMatrix.valueOf(userSize, factorSize);
            DenseMatrix trusteeDeltas = DenseMatrix.valueOf(userSize, factorSize);

            for (MatrixScalar term : scoreMatrix) {
                int trusterIndex = term.getRow(); // user userIdx
                int itemExplicitIndex = term.getColumn(); // item itemIdx
                // real rating on item itemIdx rated by user userIdx
                float score = term.getValue();
                // To speed up, directly access the prediction instead of
                // invoking "predictRating = predict(userIdx,itemIdx)"
                float userBias = userBiases.getValue(trusterIndex);
                float itemBias = itemBiases.getValue(itemExplicitIndex);
                // TODO 考虑重构减少迭代
                DenseVector trusterVector = trusterFactors.getRowVector(trusterIndex);
                DenseVector itemExplicitVector = itemExplicitFactors.getRowVector(itemExplicitIndex);
                float predict = meanScore + userBias + itemBias + scalar.dotProduct(trusterVector, itemExplicitVector).getValue();

                // get the implicit influence predict rating using items rated
                // by user userIdx
                SparseVector rateVector = scoreMatrix.getRowVector(trusterIndex);
                if (rateVector.getElementSize() > 0) {
                    float sum = 0F;
                    for (VectorScalar rateTerm : rateVector) {
                        int itemImplicitIndex = rateTerm.getIndex();
                        DenseVector itemImplicitVector = itemImplicitFactors.getRowVector(itemImplicitIndex);
                        sum += scalar.dotProduct(itemImplicitVector, itemExplicitVector).getValue();
                    }
                    predict += sum / Math.sqrt(rateVector.getElementSize());
                }

                // the user-specific influence of users (trustees)trusted by
                // user userIdx
                SparseVector socialVector = socialMatrix.getRowVector(trusterIndex);
                if (socialVector.getElementSize() > 0) {
                    float sum = 0F;
                    for (VectorScalar socialTerm : socialVector) {
                        int trusteeIndex = socialTerm.getIndex();
                        sum += scalar.dotProduct(trusteeFactors.getRowVector(trusteeIndex), itemExplicitVector).getValue();
                    }
                    predict += sum / Math.sqrt(socialVector.getElementSize());
                }
                float error = predict - score;
                totalError += error * error;

                float trusterDenominator = (float) Math.sqrt(rateVector.getElementSize());
                float trusteeDenominator = (float) Math.sqrt(socialVector.getElementSize());

                float trusterWeight = 1F / trusterDenominator;
                float itemExplicitWeight = itemWeights.getValue(itemExplicitIndex);

                // update factors
                // stochastic gradient descent sgd
                float sgd = error + regBias * trusterWeight * userBias;
                userBiases.shiftValue(trusterIndex, -learnRatio * sgd);
                sgd = error + regBias * itemExplicitWeight * itemBias;
                itemBiases.shiftValue(itemExplicitIndex, -learnRatio * sgd);
                totalError += regBias * trusterWeight * userBias * userBias + regBias * itemExplicitWeight * itemBias * itemBias;

                float[] itemSums = new float[factorSize];
                for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                    float sum = 0F;
                    for (VectorScalar rateTerm : rateVector) {
                        int itemImplicitIndex = rateTerm.getIndex();
                        sum += itemImplicitFactors.getValue(itemImplicitIndex, factorIndex);
                    }
                    itemSums[factorIndex] = trusterDenominator > 0F ? sum / trusterDenominator : sum;
                }

                float[] trusteesSums = new float[factorSize];
                for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                    float sum = 0F;
                    for (VectorScalar socialTerm : socialVector) {
                        int trusteeIndex = socialTerm.getIndex();
                        sum += trusteeFactors.getValue(trusteeIndex, factorIndex);
                    }
                    trusteesSums[factorIndex] = trusteeDenominator > 0F ? sum / trusteeDenominator : sum;
                }

                for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                    float userFactor = trusterFactors.getValue(trusterIndex, factorIndex);
                    float itemFactor = itemExplicitFactors.getValue(itemExplicitIndex, factorIndex);
                    float userDelta = error * itemFactor + userRegularization * trusterWeight * userFactor;
                    float itemDelta = error * (userFactor + itemSums[factorIndex] + trusteesSums[factorIndex]) + itemRegularization * itemExplicitWeight * itemFactor;
                    // update trusterDeltas
                    trusterDeltas.shiftValue(trusterIndex, factorIndex, userDelta);
                    // update itemExplicitFactors
                    itemExplicitFactors.shiftValue(itemExplicitIndex, factorIndex, -learnRatio * itemDelta);
                    totalError += userRegularization * trusterWeight * userFactor * userFactor + itemRegularization * itemExplicitWeight * itemFactor * itemFactor;

                    // update itemImplicitFactors
                    for (VectorScalar rateTerm : rateVector) {
                        int itemImplicitIndex = rateTerm.getIndex();
                        float itemImplicitFactor = itemImplicitFactors.getValue(itemImplicitIndex, factorIndex);
                        float itemImplicitWeight = itemWeights.getValue(itemImplicitIndex);
                        float itemImplicitDelta = error * itemFactor / trusterDenominator + itemRegularization * itemImplicitWeight * itemImplicitFactor;
                        itemImplicitFactors.shiftValue(itemImplicitIndex, factorIndex, -learnRatio * itemImplicitDelta);
                        totalError += itemRegularization * itemImplicitWeight * itemImplicitFactor * itemImplicitFactor;
                    }

                    // update trusteeDeltas
                    for (VectorScalar socialTerm : socialVector) {
                        int trusteeIndex = socialTerm.getIndex();
                        float trusteeFactor = trusteeFactors.getValue(trusteeIndex, factorIndex);
                        float trusteeWeight = trusteeWeights.getValue(trusteeIndex);
                        float trusteeDelta = error * itemFactor / trusteeDenominator + userRegularization * trusteeWeight * trusteeFactor;
                        trusteeDeltas.shiftValue(trusteeIndex, factorIndex, trusteeDelta);
                        totalError += userRegularization * trusteeWeight * trusteeFactor * trusteeFactor;
                    }
                }
            }

            for (MatrixScalar socialTerm : socialMatrix) {
                int trusterIndex = socialTerm.getRow();
                int trusteeIndex = socialTerm.getColumn();
                float score = socialTerm.getValue();
                DenseVector trusterVector = trusterFactors.getRowVector(trusterIndex);
                DenseVector trusteeVector = trusteeFactors.getRowVector(trusteeIndex);
                float predtict = scalar.dotProduct(trusterVector, trusteeVector).getValue();
                float error = predtict - score;
                totalError += socialRegularization * error * error;
                error = socialRegularization * error;

                float trusterWeight = trusterWeights.getValue(trusterIndex);
                // update trusterDeltas,trusteeDeltas
                for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                    float trusterFactor = trusterFactors.getValue(trusterIndex, factorIndex);
                    float trusteeFactor = trusteeFactors.getValue(trusteeIndex, factorIndex);
                    trusterDeltas.shiftValue(trusterIndex, factorIndex, error * trusteeFactor + socialRegularization * trusterWeight * trusterFactor);
                    trusteeDeltas.shiftValue(trusteeIndex, factorIndex, error * trusterFactor);
                    totalError += socialRegularization * trusterWeight * trusterFactor * trusterFactor;
                }
            }

            trusterFactors.iterateElement(MathCalculator.PARALLEL, (element) -> {
                int row = element.getRow();
                int column = element.getColumn();
                float value = element.getValue();
                element.setValue(value + trusterDeltas.getValue(row, column) * -learnRatio);
            });
            trusteeFactors.iterateElement(MathCalculator.PARALLEL, (element) -> {
                int row = element.getRow();
                int column = element.getColumn();
                float value = element.getValue();
                element.setValue(value + trusteeDeltas.getValue(row, column) * -learnRatio);
            });

            totalError *= 0.5F;
            if (isConverged(epocheIndex) && isConverged) {
                break;
            }
            isLearned(epocheIndex);
            currentError = totalError;
        } // end of training
    }

    /**
     * predict a specific rating for user userIdx on item itemIdx.
     *
     * @param userIndex user index
     * @param itemIndex item index
     * @return predictive rating for user userIdx on item itemIdx
     * @throws ModelException if error occurs
     */
    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        DefaultScalar scalar = DefaultScalar.getInstance();
        DenseVector trusterVector = trusterFactors.getRowVector(userIndex);
        DenseVector itemExplicitVector = itemExplicitFactors.getRowVector(itemIndex);
        float value = meanScore + userBiases.getValue(userIndex) + itemBiases.getValue(itemIndex) + scalar.dotProduct(trusterVector, itemExplicitVector).getValue();

        // the implicit influence of items rated by user in the past on the
        // ratings of unknown items in the future.
        SparseVector rateVector = scoreMatrix.getRowVector(userIndex);
        if (rateVector.getElementSize() > 0) {
            float sum = 0F;
            for (VectorScalar rateTerm : rateVector) {
                itemIndex = rateTerm.getIndex();
                // TODO 考虑重构减少迭代
                DenseVector itemImplicitVector = itemImplicitFactors.getRowVector(itemIndex);
                sum += scalar.dotProduct(itemImplicitVector, itemExplicitVector).getValue();
            }
            value += sum / Math.sqrt(rateVector.getElementSize());
        }

        // the user-specific influence of users (trustees)trusted by user u
        SparseVector socialVector = socialMatrix.getRowVector(userIndex);
        if (socialVector.getElementSize() > 0) {
            float sum = 0F;
            for (VectorScalar socialTerm : socialVector) {
                userIndex = socialTerm.getIndex();
                DenseVector trusteeVector = trusteeFactors.getRowVector(userIndex);
                sum += scalar.dotProduct(trusteeVector, itemExplicitVector).getValue();
            }
            value += sum / Math.sqrt(socialVector.getElementSize());
        }
        instance.setQuantityMark(value);
    }

}
