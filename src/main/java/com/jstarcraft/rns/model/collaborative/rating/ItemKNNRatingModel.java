package com.jstarcraft.rns.model.collaborative.rating;

import java.util.Iterator;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.rns.model.collaborative.ItemKNNModel;

/**
 * 
 * Item KNN推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class ItemKNNRatingModel extends ItemKNNModel {

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        SparseVector userVector = userVectors[userIndex];
        MathVector neighbors = itemNeighbors[itemIndex];
        if (userVector.getElementSize() == 0 || neighbors.getElementSize() == 0) {
            instance.setQuantityMark(meanScore);
            return;
        }

        float sum = 0F, absolute = 0F;
        int count = 0;
        int leftCursor = 0, rightCursor = 0, leftSize = userVector.getElementSize(), rightSize = neighbors.getElementSize();
        Iterator<VectorScalar> leftIterator = userVector.iterator();
        VectorScalar leftTerm = leftIterator.next();
        Iterator<VectorScalar> rightIterator = neighbors.iterator();
        VectorScalar rightTerm = rightIterator.next();
        // 判断两个有序数组中是否存在相同的数字
        while (leftCursor < leftSize && rightCursor < rightSize) {
            if (leftTerm.getIndex() == rightTerm.getIndex()) {
                count++;
                float correlation = rightTerm.getValue();
                float score = leftTerm.getValue();
                sum += correlation * (score - itemMeans.getValue(rightTerm.getIndex()));
                absolute += Math.abs(correlation);
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
            instance.setQuantityMark(meanScore);
            return;
        }

        instance.setQuantityMark(absolute > 0 ? itemMeans.getValue(itemIndex) + sum / absolute : meanScore);
    }

}
