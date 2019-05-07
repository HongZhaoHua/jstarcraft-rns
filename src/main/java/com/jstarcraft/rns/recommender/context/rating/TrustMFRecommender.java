package com.jstarcraft.rns.recommender.context.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.configurator.Configuration;
import com.jstarcraft.rns.exception.RecommendationException;
import com.jstarcraft.rns.recommender.SocialRecommender;
import com.jstarcraft.rns.utility.LogisticUtility;

/**
 * 
 * TrustMF推荐器
 * 
 * <pre>
 * Social Collaborative Filtering by Trust
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class TrustMFRecommender extends SocialRecommender {
    /**
     * truster model
     */
    private DenseMatrix trusterUserFactors, trusterItemFactors, trusteeUserDeltas;

    /**
     * trustee model
     */
    private DenseMatrix trusteeUserFactors, trusteeItemFactors, trusterUserDeltas;

    /**
     * model selection identifier
     */
    private String mode;

    // TODO 需要重构
    private void prepareByTruster() {
        trusterUserFactors = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
        trusterUserFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        trusteeUserDeltas = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
        trusteeUserDeltas.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        trusterItemFactors = DenseMatrix.valueOf(numberOfItems, numberOfFactors);
        trusterItemFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
    }

    // TODO 需要重构
    private void prepareByTrustee() {
        trusterUserDeltas = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
        trusterUserDeltas.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        trusteeUserFactors = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
        trusteeUserFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        trusteeItemFactors = DenseMatrix.valueOf(numberOfItems, numberOfFactors);
        trusteeItemFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
    }

    @Override
    public void prepare(Configuration configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        mode = configuration.getString("rec.social.model", "T");
        // algoName = "TrustMF (" + model + ")";
        switch (mode) {
        case "Tr":
            prepareByTruster();
            break;
        case "Te":
            prepareByTrustee();
            break;
        case "T":
        default:
            prepareByTruster();
            prepareByTrustee();
        }
    }

    /**
     * Build TrusterMF model: Br*Vr
     *
     * @throws RecommendationException if error occurs
     */
    private void trainByTruster(DefaultScalar scalar) {
        for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
            totalLoss = 0F;
            // gradients of trusterUserTrusterFactors,
            // trusterUserTrusteeFactors, trusterItemFactors
            DenseMatrix trusterGradients = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
            DenseMatrix trusteeGradients = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
            DenseMatrix itemGradients = DenseMatrix.valueOf(numberOfItems, numberOfFactors);

            // rate matrix
            for (MatrixScalar term : trainMatrix) {
                int userIndex = term.getRow();
                int itemIndex = term.getColumn();
                float rate = term.getValue();
                float predict = predict(userIndex, itemIndex);
                float error = LogisticUtility.getValue(predict) - normalize(rate);
                totalLoss += error * error;
                error = LogisticUtility.getGradient(predict) * error;
                for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
                    float trusterUserFactor = trusterUserFactors.getValue(userIndex, factorIndex);
                    float trusterItemFactor = trusterItemFactors.getValue(itemIndex, factorIndex);
                    trusterGradients.shiftValue(userIndex, factorIndex, error * trusterItemFactor + userRegularization * trusterUserFactor);
                    itemGradients.shiftValue(itemIndex, factorIndex, error * trusterUserFactor + itemRegularization * trusterItemFactor);
                    totalLoss += userRegularization * trusterUserFactor * trusterUserFactor + itemRegularization * trusterItemFactor * trusterItemFactor;
                }
            }

            // social matrix
            for (MatrixScalar term : socialMatrix) {
                int trusterIndex = term.getRow();
                int trusteeIndex = term.getColumn();
                float rate = term.getValue();
                DenseVector trusteeVector = trusteeUserDeltas.getRowVector(trusteeIndex);
                DenseVector trusterVector = trusterUserFactors.getRowVector(trusterIndex);
                float predict = scalar.dotProduct(trusteeVector, trusterVector).getValue();
                float error = LogisticUtility.getValue(predict) - rate;
                totalLoss += socialRegularization * error * error;
                error = LogisticUtility.getGradient(predict) * error;
                for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
                    float trusterUserFactor = trusterUserFactors.getValue(trusterIndex, factorIndex);
                    float trusterUserDelta = trusteeUserDeltas.getValue(trusteeIndex, factorIndex);
                    trusterGradients.shiftValue(trusterIndex, factorIndex, socialRegularization * error * trusterUserDelta + userRegularization * trusterUserFactor);
                    trusteeGradients.shiftValue(trusteeIndex, factorIndex, socialRegularization * error * trusterUserFactor + userRegularization * trusterUserDelta);
                    totalLoss += userRegularization * trusterUserFactor * trusterUserFactor + userRegularization * trusterUserDelta * trusterUserDelta;
                }
            }

            trusteeUserDeltas.iterateElement(MathCalculator.PARALLEL, (element) -> {
                int row = element.getRow();
                int column = element.getColumn();
                float value = element.getValue();
                element.setValue(value + trusteeGradients.getValue(row, column) * -learnRate);
            });
            trusterUserFactors.iterateElement(MathCalculator.PARALLEL, (element) -> {
                int row = element.getRow();
                int column = element.getColumn();
                float value = element.getValue();
                element.setValue(value + trusterGradients.getValue(row, column) * -learnRate);
            });
            trusterItemFactors.iterateElement(MathCalculator.PARALLEL, (element) -> {
                int row = element.getRow();
                int column = element.getColumn();
                float value = element.getValue();
                element.setValue(value + itemGradients.getValue(row, column) * -learnRate);
            });

            totalLoss *= 0.5F;
            if (isConverged(iterationStep) && isConverged) {
                break;
            }
            isLearned(iterationStep);
        }
    }

    /**
     * Build TrusteeMF model: We*Ve
     *
     * @throws RecommendationException if error occurs
     */
    private void trainByTrustee(DefaultScalar scalar) {
        for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
            totalLoss = 0F;
            // gradients of trusteeUserTrusterFactors,
            // trusteeUserTrusteeFactors, trusteeItemFactors
            DenseMatrix trusterGradients = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
            DenseMatrix trusteeGradients = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
            DenseMatrix itemGradients = DenseMatrix.valueOf(numberOfItems, numberOfFactors);

            // rate matrix
            for (MatrixScalar term : trainMatrix) {
                int userIndex = term.getRow();
                int itemIndex = term.getColumn();
                float rate = term.getValue();
                float predict = predict(userIndex, itemIndex);
                float error = LogisticUtility.getValue(predict) - normalize(rate);
                totalLoss += error * error;
                error = LogisticUtility.getGradient(predict) * error;
                for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
                    float trusteeUserFactor = trusteeUserFactors.getValue(userIndex, factorIndex);
                    float trusteeItemFactor = trusteeItemFactors.getValue(itemIndex, factorIndex);
                    trusteeGradients.shiftValue(userIndex, factorIndex, error * trusteeItemFactor + userRegularization * trusteeUserFactor);
                    itemGradients.shiftValue(itemIndex, factorIndex, error * trusteeUserFactor + itemRegularization * trusteeItemFactor);
                    totalLoss += userRegularization * trusteeUserFactor * trusteeUserFactor + itemRegularization * trusteeItemFactor * trusteeItemFactor;
                }
            }

            // social matrix
            for (MatrixScalar term : socialMatrix) {
                int trusterIndex = term.getRow();
                int trusteeIndex = term.getColumn();
                float rate = term.getValue();
                DenseVector trusterVector = trusterUserDeltas.getRowVector(trusterIndex);
                DenseVector trusteeVector = trusteeUserFactors.getRowVector(trusteeIndex);
                float predict = scalar.dotProduct(trusterVector, trusteeVector).getValue();
                float error = LogisticUtility.getValue(predict) - rate;
                totalLoss += socialRegularization * error * error;
                error = LogisticUtility.getGradient(predict) * error;
                for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
                    float trusteeUserFactor = trusteeUserFactors.getValue(trusteeIndex, factorIndex);
                    float trusteeUserDelta = trusterUserDeltas.getValue(trusterIndex, factorIndex);
                    trusteeGradients.shiftValue(trusteeIndex, factorIndex, socialRegularization * error * trusteeUserDelta + userRegularization * trusteeUserFactor);
                    trusterGradients.shiftValue(trusterIndex, factorIndex, socialRegularization * error * trusteeUserFactor + userRegularization * trusteeUserDelta);
                    totalLoss += userRegularization * trusteeUserFactor * trusteeUserFactor + userRegularization * trusteeUserDelta * trusteeUserDelta;
                }
            }

            trusterUserDeltas.iterateElement(MathCalculator.PARALLEL, (element) -> {
                int row = element.getRow();
                int column = element.getColumn();
                float value = element.getValue();
                element.setValue(value + trusterGradients.getValue(row, column) * -learnRate);
            });
            trusteeUserFactors.iterateElement(MathCalculator.PARALLEL, (element) -> {
                int row = element.getRow();
                int column = element.getColumn();
                float value = element.getValue();
                element.setValue(value + trusteeGradients.getValue(row, column) * -learnRate);
            });
            trusteeItemFactors.iterateElement(MathCalculator.PARALLEL, (element) -> {
                int row = element.getRow();
                int column = element.getColumn();
                float value = element.getValue();
                element.setValue(value + itemGradients.getValue(row, column) * -learnRate);
            });

            totalLoss *= 0.5D;
            if (isConverged(iterationStep) && isConverged) {
                break;
            }
            isLearned(iterationStep);
            currentLoss = totalLoss;
            currentLoss = totalLoss;
        }
    }

    @Override
    protected void doPractice() {
        DefaultScalar scalar = DefaultScalar.getInstance();
        switch (mode) {
        case "Tr":
            trainByTruster(scalar);
            break;
        case "Te":
            trainByTrustee(scalar);
            break;
        case "T":
        default:
            trainByTruster(scalar);
            trainByTrustee(scalar);
        }
    }

    /**
     * This is the method used by the paper authors
     *
     * @param iter number of iteration
     */
    @Override
    protected void isLearned(int iter) {
        // TODO 此处需要重构(修改为配置)
        if (iter == 10) {
            learnRate *= 0.6;
        } else if (iter == 30) {
            learnRate *= 0.333;
        } else if (iter == 100) {
            learnRate *= 0.5;
        }
        currentLoss = totalLoss;
    }

    @Override
    protected float predict(int userIndex, int itemIndex) {
        DefaultScalar scalar = DefaultScalar.getInstance();
        float value;
        DenseVector userVector;
        DenseVector itemVector;
        switch (mode) {
        case "Tr":
            userVector = trusterUserFactors.getRowVector(userIndex);
            itemVector = trusterItemFactors.getRowVector(itemIndex);
            value = scalar.dotProduct(userVector, itemVector).getValue();
            break;
        case "Te":
            userVector = trusteeUserFactors.getRowVector(userIndex);
            itemVector = trusteeItemFactors.getRowVector(itemIndex);
            value = scalar.dotProduct(userVector, itemVector).getValue();
            break;
        case "T":
        default:
            DenseVector trusterUserVector = trusterUserFactors.getRowVector(userIndex);
            DenseVector trusteeUserVector = trusteeUserFactors.getRowVector(userIndex);
            DenseVector trusterItemVector = trusterItemFactors.getRowVector(itemIndex);
            DenseVector trusteeItemVector = trusteeItemFactors.getRowVector(itemIndex);
            value = 0F;
            for (int index = 0; index < numberOfFactors; index++) {
                value += (trusterUserVector.getValue(index) + trusteeUserVector.getValue(index)) * (trusterItemVector.getValue(index) + trusteeItemVector.getValue(index));
            }
            value /= 4F;
        }
        return value;
    }

    @Override
    public float predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        float predict = predict(userIndex, itemIndex);
        predict = denormalize(LogisticUtility.getValue(predict));
        return predict;
    }

}
