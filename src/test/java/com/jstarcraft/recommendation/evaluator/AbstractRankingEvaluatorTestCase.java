package com.jstarcraft.recommendation.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.recommendation.recommender.Recommender;

public abstract class AbstractRankingEvaluatorTestCase extends AbstractEvaluatorTestCase<Integer> {

	@Override
	protected Collection<Integer> check(int userIndex) {
		Set<Integer> itemSet = new HashSet<>();
		int from = testPaginations[userIndex], to = testPaginations[userIndex + 1];
		for (int index = from, size = to; index < size; index++) {
			int position = testPositions[index];
			itemSet.add(testMarker.getDiscreteFeature(itemDimension, position));
		}
		return itemSet;
	}

	@Override
	protected List<KeyValue<Integer, Float>> recommend(Recommender recommender, int userIndex) {
		Set<Integer> itemSet = new HashSet<>();
		int from = trainPaginations[userIndex], to = trainPaginations[userIndex + 1];
		for (int index = from, size = to; index < size; index++) {
			int position = trainPositions[index];
			itemSet.add(trainMarker.getDiscreteFeature(itemDimension, position));
		}
		int[] discreteFeatures = new int[trainMarker.getDiscreteOrder()];
		float[] continuousFeatures = new float[trainMarker.getContinuousOrder()];
		if (from < to) {
			int position = trainPositions[to - 1];
			for (int dimension = 0, size = trainMarker.getDiscreteOrder(); dimension < size; dimension++) {
				discreteFeatures[dimension] = trainMarker.getDiscreteFeature(dimension, position);
			}
			for (int dimension = 0, size = trainMarker.getContinuousOrder(); dimension < size; dimension++) {
				continuousFeatures[dimension] = trainMarker.getContinuousFeature(dimension, position);
			}
		}
		List<KeyValue<Integer, Float>> recommendList = new ArrayList<>(numberOfItems - itemSet.size());
		for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
			if (itemSet.contains(itemIndex)) {
				continue;
			}
			discreteFeatures[itemDimension] = itemIndex;
			recommendList.add(new KeyValue<>(itemIndex, recommender.predict(discreteFeatures, continuousFeatures)));
		}
		Collections.sort(recommendList, (left, right) -> {
			return right.getValue().compareTo(left.getValue());
		});
		return recommendList;
	}

}
