package com.jstarcraft.rns.recommender.collaborative.rating;

import java.util.Iterator;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.rns.recommender.collaborative.ItemKNNRecommender;

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
public class ItemKNNRatingRecommender extends ItemKNNRecommender {

	@Override
	public float predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
		SparseVector userVector = userVectors[userIndex];
		int[] neighbors = itemNeighbors[itemIndex];
		if (userVector.getElementSize() == 0 || neighbors == null) {
			return meanOfScore;
		}

		float sum = 0F, absolute = 0F;
		int count = 0;
		int leftIndex = 0, rightIndex = 0, leftSize = userVector.getElementSize(), rightSize = neighbors.length;
		Iterator<VectorScalar> iterator = userVector.iterator();
		VectorScalar term = iterator.next();
		// 判断两个有序数组中是否存在相同的数字
		while (leftIndex < leftSize && rightIndex < rightSize) {
			if (term.getIndex() == neighbors[rightIndex]) {
				count++;
				double similarity = similarityMatrix.getValue(itemIndex, neighbors[rightIndex]);
				double rate = term.getValue();
				sum += similarity * (rate - itemMeans.getValue(neighbors[rightIndex]));
				absolute += Math.abs(similarity);
				if (iterator.hasNext()) {
					term = iterator.next();
				}
				leftIndex++;
				rightIndex++;
			} else if (term.getIndex() > neighbors[rightIndex]) {
				rightIndex++;
			} else if (term.getIndex() < neighbors[rightIndex]) {
				if (iterator.hasNext()) {
					term = iterator.next();
				}
				leftIndex++;
			}
		}

		if (count == 0) {
			return meanOfScore;
		}

		return absolute > 0 ? itemMeans.getValue(itemIndex) + sum / absolute : meanOfScore;
	}

}
