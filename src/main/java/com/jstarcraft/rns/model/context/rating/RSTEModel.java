package com.jstarcraft.rns.model.context.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.common.configuration.Option;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.model.SocialModel;
import com.jstarcraft.rns.utility.LogisticUtility;

/**
 * 
 * RSTE推荐器
 * 
 * <pre>
 * Learning to Recommend with Social Trust Ensemble
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class RSTEModel extends SocialModel {
    private float userSocialRatio;

    @Override
    public void prepare(Option configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        userFactors = DenseMatrix.valueOf(userSize, factorSize);
        userFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        itemFactors = DenseMatrix.valueOf(itemSize, factorSize);
        itemFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        userSocialRatio = configuration.getFloat("recommender.user.social.ratio", 0.8F);
    }

    @Override
    protected void doPractice() {
        DefaultScalar scalar = DefaultScalar.getInstance();
        DenseVector socialFactors = DenseVector.valueOf(factorSize);
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            totalError = 0F;
            DenseMatrix userDeltas = DenseMatrix.valueOf(userSize, factorSize);
            DenseMatrix itemDeltas = DenseMatrix.valueOf(itemSize, factorSize);

            // ratings
            for (int userIndex = 0; userIndex < userSize; userIndex++) {
                SparseVector socialVector = socialMatrix.getRowVector(userIndex);
                float socialWeight = 0F;
                socialFactors.setValues(0F);
                for (VectorScalar socialTerm : socialVector) {
                    for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                        socialFactors.setValue(factorIndex, socialFactors.getValue(factorIndex) + socialTerm.getValue() * userFactors.getValue(socialTerm.getIndex(), factorIndex));
                    }
                    socialWeight += socialTerm.getValue();
                }
                DenseVector userVector = userFactors.getRowVector(userIndex);
                for (VectorScalar rateTerm : scoreMatrix.getRowVector(userIndex)) {
                    int itemIndex = rateTerm.getIndex();
                    float score = rateTerm.getValue();
                    score = (score - minimumScore) / (maximumScore - minimumScore);
                    // compute directly to speed up calculation
                    DenseVector itemVector = itemFactors.getRowVector(itemIndex);
                    float predict = scalar.dotProduct(userVector, itemVector).getValue();
                    float sum = 0F;
                    for (VectorScalar socialTerm : socialVector) {
                        sum += socialTerm.getValue() * scalar.dotProduct(userFactors.getRowVector(socialTerm.getIndex()), itemVector).getValue();
                    }
                    predict = userSocialRatio * predict + (1F - userSocialRatio) * (socialWeight > 0F ? sum / socialWeight : 0F);
                    float error = LogisticUtility.getValue(predict) - score;
                    totalError += error * error;
                    error = LogisticUtility.getGradient(predict) * error;
                    for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                        float userFactor = userFactors.getValue(userIndex, factorIndex);
                        float itemFactor = itemFactors.getValue(itemIndex, factorIndex);
                        float userDelta = userSocialRatio * error * itemFactor + userRegularization * userFactor;
                        float socialFactor = socialWeight > 0 ? socialFactors.getValue(factorIndex) / socialWeight : 0;
                        float itemDelta = error * (userSocialRatio * userFactor + (1 - userSocialRatio) * socialFactor) + itemRegularization * itemFactor;
                        userDeltas.shiftValue(userIndex, factorIndex, userDelta);
                        itemDeltas.shiftValue(itemIndex, factorIndex, itemDelta);
                        totalError += userRegularization * userFactor * userFactor + itemRegularization * itemFactor * itemFactor;
                    }
                }
            }

            // social
            for (int trusterIndex = 0; trusterIndex < userSize; trusterIndex++) {
                SparseVector trusterVector = socialMatrix.getColumnVector(trusterIndex);
                for (VectorScalar term : trusterVector) {
                    int trusteeIndex = term.getIndex();
                    SparseVector trusteeVector = socialMatrix.getRowVector(trusteeIndex);
                    DenseVector userVector = userFactors.getRowVector(trusteeIndex);
                    float socialWeight = 0F;
                    for (VectorScalar socialTerm : trusteeVector) {
                        socialWeight += socialTerm.getValue();
                    }
                    for (VectorScalar rateTerm : scoreMatrix.getRowVector(trusteeIndex)) {
                        int itemIndex = rateTerm.getIndex();
                        float score = rateTerm.getValue();
                        score = (score - minimumScore) / (maximumScore - minimumScore);
                        // compute prediction for user-item (p, j)
                        DenseVector itemVector = itemFactors.getRowVector(itemIndex);
                        float predict = scalar.dotProduct(userVector, itemVector).getValue();
                        float sum = 0F;
                        for (VectorScalar socialTerm : trusteeVector) {
                            sum += socialTerm.getValue() * scalar.dotProduct(itemFactors.getRowVector(socialTerm.getIndex()), itemVector).getValue();
                        }
                        predict = userSocialRatio * predict + (1F - userSocialRatio) * (socialWeight > 0F ? sum / socialWeight : 0F);
                        // double pred = predict(p, j, false);
                        float error = LogisticUtility.getValue(predict) - score;
                        error = LogisticUtility.getGradient(predict) * error * term.getValue();
                        for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                            userDeltas.shiftValue(trusterIndex, factorIndex, (1 - userSocialRatio) * error * itemFactors.getValue(itemIndex, factorIndex));
                        }
                    }
                }
            }
            userFactors.iterateElement(MathCalculator.PARALLEL, (element) -> {
                int row = element.getRow();
                int column = element.getColumn();
                float value = element.getValue();
                element.setValue(value + userDeltas.getValue(row, column) * -learnRatio);
            });
            itemFactors.iterateElement(MathCalculator.PARALLEL, (element) -> {
                int row = element.getRow();
                int column = element.getColumn();
                float value = element.getValue();
                element.setValue(value + itemDeltas.getValue(row, column) * -learnRatio);
            });

            totalError *= 0.5F;
            if (isConverged(epocheIndex) && isConverged) {
                break;
            }
            isLearned(epocheIndex);
            currentError = totalError;
        }
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        DefaultScalar scalar = DefaultScalar.getInstance();
        DenseVector userVector = userFactors.getRowVector(userIndex);
        DenseVector itemVector = itemFactors.getRowVector(itemIndex);
        float predict = scalar.dotProduct(userVector, itemVector).getValue();
        float sum = 0F, socialWeight = 0F;
        SparseVector socialVector = socialMatrix.getRowVector(userIndex);
        for (VectorScalar soicalTerm : socialVector) {
            float score = soicalTerm.getValue();
            DenseVector soicalFactor = userFactors.getRowVector(soicalTerm.getIndex());
            sum += score * scalar.dotProduct(soicalFactor, itemVector).getValue();
            socialWeight += score;
        }
        predict = userSocialRatio * predict + (1 - userSocialRatio) * (socialWeight > 0 ? sum / socialWeight : 0);
        instance.setQuantityMark(denormalize(LogisticUtility.getValue(predict)));
    }

}
