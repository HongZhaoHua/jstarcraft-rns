package com.jstarcraft.rns.model.context.rating;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.algorithm.similarity.Similarity;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.matrix.SymmetryMatrix;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.common.reflection.ReflectionUtility;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.model.SocialRecommender;

/**
 * 
 * SoReg推荐器
 * 
 * <pre>
 * Recommender systems with social regularization
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class SoRegRecommender extends SocialRecommender {

    private SymmetryMatrix socialCorrelations;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        userFactors = DenseMatrix.valueOf(userSize, factorSize);
        userFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        itemFactors = DenseMatrix.valueOf(itemSize, factorSize);
        itemFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });

        // TODO 修改为配置枚举
        try {
            Class<Similarity> similarityClass = (Class<Similarity>) Class.forName(configuration.getString("recommender.similarity.class"));
            Similarity similarity = ReflectionUtility.getInstance(similarityClass);
            socialCorrelations = similarity.makeSimilarityMatrix(socialMatrix, false, configuration.getFloat("recommender.similarity.shrinkage", 0F));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        for (MatrixScalar term : socialCorrelations) {
            float similarity = term.getValue();
            if (similarity == 0F) {
                continue;
            }
            similarity = (1F + similarity) / 2F;
            term.setValue(similarity);
        }
    }

    @Override
    protected void doPractice() {
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            totalError = 0F;
            DenseMatrix userDeltas = DenseMatrix.valueOf(userSize, factorSize);
            DenseMatrix itemDeltas = DenseMatrix.valueOf(itemSize, factorSize);

            // ratings
            for (MatrixScalar term : scoreMatrix) {
                int userIndex = term.getRow();
                int itemIndex = term.getColumn();
                float error = predict(userIndex, itemIndex) - term.getValue();
                totalError += error * error;
                for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                    float userFactorValue = userFactors.getValue(userIndex, factorIndex);
                    float itemFactorValue = itemFactors.getValue(itemIndex, factorIndex);
                    userDeltas.shiftValue(userIndex, factorIndex, error * itemFactorValue + userRegularization * userFactorValue);
                    itemDeltas.shiftValue(itemIndex, factorIndex, error * userFactorValue + itemRegularization * itemFactorValue);
                    totalError += userRegularization * userFactorValue * userFactorValue + itemRegularization * itemFactorValue * itemFactorValue;
                }
            }

            // friends
            for (int userIndex = 0; userIndex < userSize; userIndex++) {
                // out links: F+
                SparseVector trusterVector = socialMatrix.getRowVector(userIndex);
                for (VectorScalar term : trusterVector) {
                    int trusterIndex = term.getIndex();
                    float trusterSimilarity = socialCorrelations.getValue(userIndex, trusterIndex);
                    if (!Float.isNaN(trusterSimilarity)) {
                        for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                            float userFactor = userFactors.getValue(userIndex, factorIndex) - userFactors.getValue(trusterIndex, factorIndex);
                            userDeltas.shiftValue(userIndex, factorIndex, socialRegularization * trusterSimilarity * userFactor);
                            totalError += socialRegularization * trusterSimilarity * userFactor * userFactor;
                        }
                    }
                }

                // in links: F-
                SparseVector trusteeVector = socialMatrix.getColumnVector(userIndex);
                for (VectorScalar term : trusteeVector) {
                    int trusteeIndex = term.getIndex();
                    float trusteeSimilarity = socialCorrelations.getValue(userIndex, trusteeIndex);
                    if (!Float.isNaN(trusteeSimilarity)) {
                        for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                            float userFactor = userFactors.getValue(userIndex, factorIndex) - userFactors.getValue(trusteeIndex, factorIndex);
                            userDeltas.shiftValue(userIndex, factorIndex, socialRegularization * trusteeSimilarity * userFactor);
                            totalError += socialRegularization * trusteeSimilarity * userFactor * userFactor;
                        }
                    }
                }
            }

            // end of for loop
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

            totalError *= 0.5F;
            if (isConverged(epocheIndex) && isConverged) {
                break;
            }
            isLearned(epocheIndex);
            currentError = totalError;
        }
    }
    
    @Override
    protected float predict(int userIndex, int itemIndex) {
        float score = super.predict(userIndex, itemIndex);
        if (score > maximumScore) {
            score = maximumScore;
        } else if (score < minimumScore) {
            score = minimumScore;
        }
        return score;
    }

}
