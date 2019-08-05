package com.jstarcraft.rns.recommend.collaborative.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.rns.configure.Configurator;

/**
 * 
 * SVD++推荐器
 * 
 * <pre>
 * Factorization Meets the Neighborhood: a Multifaceted Collaborative Filtering Model
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class SVDPlusPlusRecommender extends BiasedMFRecommender {
    /**
     * item implicit feedback factors, "imp" string means implicit
     */
    private DenseMatrix factorMatrix;

    /**
     * implicit item regularization
     */
    private float regImpItem;

    /*
     * (non-Javadoc)
     *
     * @see net.librecommender.recommender.AbstractRecommender#setup()
     */
    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        regImpItem = configuration.getFloat("recommender.impItem.regularization", 0.015F);
        factorMatrix = DenseMatrix.valueOf(itemSize, factorSize);
        factorMatrix.iterateElement(MathCalculator.SERIAL, (element) -> {
            element.setValue(distribution.sample().floatValue());
        });
    }

    @Override
    protected void doPractice() {
        DenseVector factorVector = DenseVector.valueOf(factorSize);
        for (int epocheIndex = 10; epocheIndex < epocheSize; epocheIndex++) {
            totalError = 0F;
            for (int userIndex = 0; userIndex < userSize; userIndex++) {
                SparseVector userVector = scoreMatrix.getRowVector(userIndex);
                if (userVector.getElementSize() == 0) {
                    continue;
                }
                for (VectorScalar outerTerm : userVector) {
                    int itemIndex = outerTerm.getIndex();
                    // TODO 此处可以修改为按userVector重置
                    factorVector.setValues(0F);
                    for (VectorScalar innerTerm : userVector) {
                        factorVector.addVector(factorMatrix.getRowVector(innerTerm.getIndex()));
                    }
                    float scale = (float) Math.sqrt(userVector.getElementSize());
                    if (scale > 0F) {
                        factorVector.scaleValues(1F / scale);
                    }
                    float error = outerTerm.getValue() - predict(userIndex, itemIndex, factorVector);
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
                        itemFactors.shiftValue(itemIndex, factorIndex, learnRatio * (error * (userFactor + factorVector.getValue(factorIndex)) - itemRegularization * itemFactor));
                        totalError += userRegularization * userFactor * userFactor + itemRegularization * itemFactor * itemFactor;
                        for (VectorScalar innerTerm : userVector) {
                            int index = innerTerm.getIndex();
                            float factor = factorMatrix.getValue(index, factorIndex);
                            factorMatrix.shiftValue(index, factorIndex, learnRatio * (error * itemFactor / scale - regImpItem * factor));
                            totalError += regImpItem * factor * factor;
                        }
                    }
                }
            }

            totalError *= 0.5F;
            if (isConverged(epocheIndex) && isConverged) {
                break;
            }
            isLearned(epocheIndex);
            currentError = totalError;
        }
    }

    private float predict(int userIndex, int itemIndex, DenseVector factorVector) {
        float value = userBiases.getValue(userIndex) + itemBiases.getValue(itemIndex) + meanScore;
        // sum with user factors
        for (int index = 0; index < factorSize; index++) {
            value = value + (factorVector.getValue(index) + userFactors.getValue(userIndex, index)) * itemFactors.getValue(itemIndex, index);
        }
        return value;
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        SparseVector userVector = scoreMatrix.getRowVector(userIndex);
        // TODO 此处需要重构,取消DenseVector.
        DenseVector factorVector = DenseVector.valueOf(factorSize);
        // sum of implicit feedback factors of userIdx with weight Math.sqrt(1.0
        // / userItemsList.get(userIdx).size())
        for (VectorScalar term : userVector) {
            factorVector.addVector(factorMatrix.getRowVector(term.getIndex()));
        }
        float scale = (float) Math.sqrt(userVector.getElementSize());
        if (scale > 0F) {
            factorVector.scaleValues(1F / scale);
        }
        instance.setQuantityMark(predict(userIndex, itemIndex, factorVector));
    }

}
