package com.jstarcraft.recommendation.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.utility.Int2FloatKeyValue;
import com.jstarcraft.recommendation.recommender.Recommender;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public abstract class AbstractRankingEvaluatorTestCase extends AbstractEvaluatorTestCase<IntCollection> {

	@Override
	protected IntCollection check(int userIndex) {
	    DataInstance instance = testMarker.getInstance(0);
		IntSet itemSet = new IntOpenHashSet();
		int from = testPaginations[userIndex], to = testPaginations[userIndex + 1];
		for (int index = from, size = to; index < size; index++) {
			int position = testPositions[index];
			instance.setCursor(position);
			itemSet.add(instance.getQualityFeature(itemDimension));
		}
		return itemSet;
	}

	@Override
	protected List<Int2FloatKeyValue> recommend(Recommender recommender, int userIndex) {
	    DataInstance instance = trainMarker.getInstance(0);
		Set<Integer> itemSet = new HashSet<>();
		int from = trainPaginations[userIndex], to = trainPaginations[userIndex + 1];
		for (int index = from, size = to; index < size; index++) {
			int position = trainPositions[index];
			instance.setCursor(position);
			itemSet.add(instance.getQualityFeature(itemDimension));
		}
		int[] discreteFeatures = new int[trainMarker.getQualityOrder()];
		float[] continuousFeatures = new float[trainMarker.getQuantityOrder()];
		if (from < to) {
			int position = trainPositions[to - 1];
			for (int dimension = 0, size = trainMarker.getQualityOrder(); dimension < size; dimension++) {
				discreteFeatures[dimension] = instance.getQualityFeature(dimension);
			}
			for (int dimension = 0, size = trainMarker.getQuantityOrder(); dimension < size; dimension++) {
				continuousFeatures[dimension] = instance.getQuantityFeature(dimension);
			}
		}
		List<Int2FloatKeyValue> recommendList = new ArrayList<>(numberOfItems - itemSet.size());
		for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
			if (itemSet.contains(itemIndex)) {
				continue;
			}
			discreteFeatures[itemDimension] = itemIndex;
			recommendList.add(new Int2FloatKeyValue(itemIndex, recommender.predict(discreteFeatures, continuousFeatures)));
		}
		Collections.sort(recommendList, (left, right) -> {
			return Float.compare(right.getValue(), left.getValue());
		});
		return recommendList;
	}

}
