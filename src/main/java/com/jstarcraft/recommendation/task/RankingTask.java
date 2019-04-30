package com.jstarcraft.recommendation.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.utility.Int2FloatKeyValue;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.evaluator.Evaluator;
import com.jstarcraft.recommendation.evaluator.ranking.AUCEvaluator;
import com.jstarcraft.recommendation.evaluator.ranking.MAPEvaluator;
import com.jstarcraft.recommendation.evaluator.ranking.MRREvaluator;
import com.jstarcraft.recommendation.evaluator.ranking.NDCGEvaluator;
import com.jstarcraft.recommendation.evaluator.ranking.NoveltyEvaluator;
import com.jstarcraft.recommendation.evaluator.ranking.PrecisionEvaluator;
import com.jstarcraft.recommendation.evaluator.ranking.RecallEvaluator;
import com.jstarcraft.recommendation.recommender.Recommender;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * 排序任务
 * 
 * @author Birdy
 *
 */
public class RankingTask extends AbstractTask<IntCollection> {

	public RankingTask(Class<? extends Recommender> clazz, Configuration configuration) {
		super(clazz, configuration);
	}

	@Override
	protected Collection<Evaluator> getEvaluators(SparseMatrix featureMatrix) {
		Collection<Evaluator> evaluators = new LinkedList<>();
		int size = configuration.getInteger("rec.recommender.ranking.topn", 10);
		evaluators.add(new AUCEvaluator(size));
		evaluators.add(new MAPEvaluator(size));
		evaluators.add(new MRREvaluator(size));
		evaluators.add(new NDCGEvaluator(size));
		evaluators.add(new NoveltyEvaluator(size, featureMatrix));
		evaluators.add(new PrecisionEvaluator(size));
		evaluators.add(new RecallEvaluator(size));
		return evaluators;
	}

	@Override
	protected IntCollection check(int userIndex) {
		IntSet itemSet = new IntOpenHashSet();
		int from = testPaginations[userIndex], to = testPaginations[userIndex + 1];
		for (int index = from, size = to; index < size; index++) {
			int position = testPositions[index];
			itemSet.add(testMarker.getQualityFeature(itemDimension, position));
		}
		return itemSet;
	}

	@Override
	protected List<Int2FloatKeyValue> recommend(Recommender recommender, int userIndex) {
		Set<Integer> itemSet = new HashSet<>();
		int from = trainPaginations[userIndex], to = trainPaginations[userIndex + 1];
		for (int index = from, size = to; index < size; index++) {
			int position = trainPositions[index];
			itemSet.add(trainMarker.getQualityFeature(itemDimension, position));
		}
		int[] discreteFeatures = new int[trainMarker.getQualityOrder()];
		float[] continuousFeatures = new float[trainMarker.getQuantityOrder()];
		if (from < to) {
			int position = trainPositions[to - 1];
			for (int dimension = 0, size = trainMarker.getQualityOrder(); dimension < size; dimension++) {
				discreteFeatures[dimension] = trainMarker.getQualityFeature(dimension, position);
			}
			for (int dimension = 0, size = trainMarker.getQuantityOrder(); dimension < size; dimension++) {
				continuousFeatures[dimension] = trainMarker.getQuantityFeature(dimension, position);
			}
		}
		discreteFeatures[userDimension] = userIndex;
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
