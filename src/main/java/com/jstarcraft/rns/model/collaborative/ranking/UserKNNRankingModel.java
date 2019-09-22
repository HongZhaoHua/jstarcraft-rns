package com.jstarcraft.rns.model.collaborative.ranking;

import java.util.Iterator;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.rns.model.collaborative.UserKNNModel;

/**
 * 
 * User KNN推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class UserKNNRankingModel extends UserKNNModel {

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        SparseVector itemVector = itemVectors[itemIndex];
        MathVector neighbors = userNeighbors[userIndex];
        if (itemVector.getElementSize() == 0 || neighbors == null) {
            instance.setQuantityMark(0F);
            return;
        }

        float sum = 0F, absolute = 0F;
        int count = 0;
        int leftCursor = 0, rightCursor = 0, leftSize = itemVector.getElementSize(), rightSize = neighbors.getElementSize();
        Iterator<VectorScalar> leftIterator = itemVector.iterator();
        VectorScalar leftTerm = leftIterator.next();
        Iterator<VectorScalar> rightIterator = neighbors.iterator();
        VectorScalar rightTerm = rightIterator.next();
        // 判断两个有序数组中是否存在相同的数字
        while (leftCursor < leftSize && rightCursor < rightSize) {
            if (leftTerm.getIndex() == rightTerm.getIndex()) {
                count++;
                sum += rightTerm.getValue();
                if (leftIterator.hasNext()) {
                    leftTerm = leftIterator.next();
                }
                if (rightIterator.hasNext()) {
                    rightTerm = rightIterator.next();
                }
                leftCursor++;
                rightCursor++;
            } else if (leftTerm.getIndex() > rightTerm.getIndex()) {
                if (rightIterator.hasNext()) {
                    rightTerm = rightIterator.next();
                }
                rightCursor++;
            } else if (leftTerm.getIndex() < rightTerm.getIndex()) {
                if (leftIterator.hasNext()) {
                    leftTerm = leftIterator.next();
                }
                leftCursor++;
            }
        }

        if (count == 0) {
            instance.setQuantityMark(0F);
            return;
        }

        instance.setQuantityMark(sum);
    }

}
