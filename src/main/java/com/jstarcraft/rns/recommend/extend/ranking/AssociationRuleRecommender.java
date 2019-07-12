package com.jstarcraft.rns.recommend.extend.ranking;

import java.util.Iterator;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.rns.configure.Configuration;
import com.jstarcraft.rns.recommend.AbstractRecommender;
import com.jstarcraft.rns.recommend.exception.RecommendException;

/**
 * 
 * Association Rule推荐器
 * 
 * <pre>
 * A Recommendation Algorithm Using Multi-Level Association Rules
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class AssociationRuleRecommender extends AbstractRecommender {

    /**
     * confidence matrix of association rules
     */
    private DenseMatrix associationMatrix;

    /**
     * setup
     *
     * @throws RecommendException if error occurs
     */
    @Override
    public void prepare(Configuration configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        associationMatrix = DenseMatrix.valueOf(numberOfItems, numberOfItems);
    }

    @Override
    protected void doPractice() {
        // simple rule: X => Y, given that each user vector is regarded as a
        // transaction
        for (int leftItemIndex = 0; leftItemIndex < numberOfItems; leftItemIndex++) {
            // all transactions for item itemIdx
            SparseVector leftVector = scoreMatrix.getColumnVector(leftItemIndex);
            int size = leftVector.getElementSize();
            for (int rightItemIndex = 0; rightItemIndex < numberOfItems; rightItemIndex++) {
                SparseVector rightVector = scoreMatrix.getColumnVector(rightItemIndex);
                int leftIndex = 0, rightIndex = 0, leftSize = size, rightSize = rightVector.getElementSize();
                if (leftSize != 0 && rightSize != 0) {
                    // compute confidence where containing item assoItemIdx
                    // among
                    // userRatingsVector
                    int count = 0;
                    Iterator<VectorScalar> leftIterator = leftVector.iterator();
                    Iterator<VectorScalar> rightIterator = rightVector.iterator();
                    VectorScalar leftTerm = leftIterator.next();
                    VectorScalar rightTerm = rightIterator.next();
                    // 判断两个有序数组中是否存在相同的数字
                    while (leftIndex < leftSize && rightIndex < rightSize) {
                        if (leftTerm.getIndex() == rightTerm.getIndex()) {
                            count++;
                            if (leftIterator.hasNext()) {
                                leftTerm = leftIterator.next();
                            }
                            if (rightIterator.hasNext()) {
                                rightTerm = rightIterator.next();
                            }
                            leftIndex++;
                            rightIndex++;
                        } else if (leftTerm.getIndex() > rightTerm.getIndex()) {
                            if (rightIterator.hasNext()) {
                                rightTerm = rightIterator.next();
                            }
                            rightIndex++;
                        } else if (leftTerm.getIndex() < rightTerm.getIndex()) {
                            if (leftIterator.hasNext()) {
                                leftTerm = leftIterator.next();
                            }
                            leftIndex++;
                        }
                    }
                    float value = (count + 0F) / size;
                    associationMatrix.setValue(leftItemIndex, rightItemIndex, value);
                }
            }
        }
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
    public float predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        float value = 0F;
        for (VectorScalar term : scoreMatrix.getRowVector(userIndex)) {
            int associationIndex = term.getIndex();
            float association = associationMatrix.getValue(associationIndex, itemIndex);
            double rate = term.getValue();
            value += rate * association;
        }
        return value;
    }

}
