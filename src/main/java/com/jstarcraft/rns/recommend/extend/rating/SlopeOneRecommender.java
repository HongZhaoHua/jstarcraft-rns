package com.jstarcraft.rns.recommend.extend.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.rns.configure.Configuration;
import com.jstarcraft.rns.recommend.AbstractRecommender;
import com.jstarcraft.rns.recommend.exception.RecommendException;

/**
 * 
 * Slope One推荐器
 * 
 * <pre>
 * Slope One Predictors for Online Rating-Based Collaborative Filtering
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class SlopeOneRecommender extends AbstractRecommender {
    /**
     * matrices for item-item differences with number of occurrences/cardinal
     */
    private DenseMatrix deviationMatrix, cardinalMatrix;

    /**
     * initialization
     *
     * @throws RecommendException if error occurs
     */
    @Override
    public void prepare(Configuration configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        deviationMatrix = DenseMatrix.valueOf(numberOfItems, numberOfItems);
        cardinalMatrix = DenseMatrix.valueOf(numberOfItems, numberOfItems);
    }

    /**
     * train model
     *
     * @throws RecommendException if error occurs
     */
    @Override
    protected void doPractice() {
        // compute items' differences
        for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
            SparseVector itemVector = scoreMatrix.getRowVector(userIndex);
            for (VectorScalar leftTerm : itemVector) {
                float leftRate = leftTerm.getValue();
                for (VectorScalar rightTerm : itemVector) {
                    if (leftTerm.getIndex() != rightTerm.getIndex()) {
                        float rightRate = rightTerm.getValue();
                        deviationMatrix.shiftValue(leftTerm.getIndex(), rightTerm.getIndex(), leftRate - rightRate);
                        cardinalMatrix.shiftValue(leftTerm.getIndex(), rightTerm.getIndex(), 1);
                    }
                }
            }
        }

        // normalize differences
        deviationMatrix.iterateElement(MathCalculator.PARALLEL, (scalar) -> {
            int row = scalar.getRow();
            int column = scalar.getColumn();
            float value = scalar.getValue();
            float cardinal = cardinalMatrix.getValue(row, column);
            scalar.setValue(cardinal > 0F ? value / cardinal : value);
        });
    }

    /**
     * predict a specific rating for user userIdx on item itemIdx.
     *
     * @param userIndex user index
     * @param itemIndex item index
     * @return predictive rating for user userIdx on item itemIdx
     * @throws RecommendException if error occurs
     */
    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        SparseVector userVector = scoreMatrix.getRowVector(userIndex);
        float value = 0F, sum = 0F;
        for (VectorScalar term : userVector) {
            if (itemIndex == term.getIndex()) {
                continue;
            }
            double cardinary = cardinalMatrix.getValue(itemIndex, term.getIndex());
            if (cardinary > 0F) {
                value += (deviationMatrix.getValue(itemIndex, term.getIndex()) + term.getValue()) * cardinary;
                sum += cardinary;
            }
        }
        instance.setQuantityMark(sum > 0F ? value / sum : meanOfScore);
    }

}
