package com.jstarcraft.rns.model.context.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.common.configuration.Option;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.model.SocialModel;
import com.jstarcraft.rns.utility.LogisticUtility;

/**
 * 
 * SocialMF推荐器
 * 
 * <pre>
 * A matrix factorization technique with trust propagation for recommendation in social networks
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class SocialMFModel extends SocialModel {

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
    }

    // TODO 需要重构
    @Override
    protected void doPractice() {
        DenseVector socialFactors = DenseVector.valueOf(factorSize);
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            totalError = 0F;
            DenseMatrix userDeltas = DenseMatrix.valueOf(userSize, factorSize);
            DenseMatrix itemDeltas = DenseMatrix.valueOf(itemSize, factorSize);

            // rated items
            for (MatrixScalar term : scoreMatrix) {
                int userIndex = term.getRow();
                int itemIndex = term.getColumn();
                float score = term.getValue();
                float predict = super.predict(userIndex, itemIndex);
                float error = LogisticUtility.getValue(predict) - normalize(score);
                totalError += error * error;
                error = LogisticUtility.getGradient(predict) * error;
                for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                    float userFactor = userFactors.getValue(userIndex, factorIndex);
                    float itemFactor = itemFactors.getValue(itemIndex, factorIndex);
                    userDeltas.shiftValue(userIndex, factorIndex, error * itemFactor + userRegularization * userFactor);
                    itemDeltas.shiftValue(itemIndex, factorIndex, error * userFactor + itemRegularization * itemFactor);
                    totalError += userRegularization * userFactor * userFactor + itemRegularization * itemFactor * itemFactor;
                }
            }

            // social regularization
            for (int userIndex = 0; userIndex < userSize; userIndex++) {
                SparseVector trusterVector = socialMatrix.getRowVector(userIndex);
                int numTrusters = trusterVector.getElementSize();
                if (numTrusters == 0) {
                    continue;
                }
                socialFactors.setValues(0F);
                for (VectorScalar trusterTerm : trusterVector) {
                    int trusterIndex = trusterTerm.getIndex();
                    for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                        socialFactors.setValue(factorIndex, socialFactors.getValue(factorIndex) + trusterTerm.getValue() * userFactors.getValue(trusterIndex, factorIndex));
                    }
                }
                for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                    float error = userFactors.getValue(userIndex, factorIndex) - socialFactors.getValue(factorIndex) / numTrusters;
                    userDeltas.shiftValue(userIndex, factorIndex, socialRegularization * error);
                    totalError += socialRegularization * error * error;
                }

                // those who trusted user u
                SparseVector trusteeVector = socialMatrix.getColumnVector(userIndex);
                int numTrustees = trusteeVector.getElementSize();
                for (VectorScalar trusteeTerm : trusteeVector) {
                    int trusteeIndex = trusteeTerm.getIndex();
                    trusterVector = socialMatrix.getRowVector(trusteeIndex);
                    numTrusters = trusterVector.getElementSize();
                    if (numTrusters == 0) {
                        continue;
                    }
                    socialFactors.setValues(0F);
                    for (VectorScalar trusterTerm : trusterVector) {
                        int trusterIndex = trusterTerm.getIndex();
                        for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                            socialFactors.setValue(factorIndex, socialFactors.getValue(factorIndex) + trusterTerm.getValue() * userFactors.getValue(trusterIndex, factorIndex));
                        }
                    }
                    for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                        userDeltas.shiftValue(userIndex, factorIndex, -socialRegularization * (trusteeTerm.getValue() / numTrustees) * (userFactors.getValue(trusteeIndex, factorIndex) - socialFactors.getValue(factorIndex) / numTrusters));
                    }
                }
            }
            // update user factors
            userFactors.iterateElement(MathCalculator.PARALLEL, (scalar) -> {
                int row = scalar.getRow();
                int column = scalar.getColumn();
                float value = scalar.getValue();
                scalar.setValue(value + userDeltas.getValue(row, column) * -learnRatio);
            });
            itemFactors.iterateElement(MathCalculator.PARALLEL, (scalar) -> {
                int row = scalar.getRow();
                int column = scalar.getColumn();
                float value = scalar.getValue();
                scalar.setValue(value + itemDeltas.getValue(row, column) * -learnRatio);
            });

            totalError *= 0.5D;
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
        float predict = super.predict(userIndex, itemIndex);
        instance.setQuantityMark(denormalize(LogisticUtility.getValue(predict)));
    }

}
