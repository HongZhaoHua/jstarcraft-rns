package com.jstarcraft.recommendation.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.evaluator.Evaluator;
import com.jstarcraft.recommendation.evaluator.rating.MAEEvaluator;
import com.jstarcraft.recommendation.evaluator.rating.MPEEvaluator;
import com.jstarcraft.recommendation.evaluator.rating.MSEEvaluator;
import com.jstarcraft.recommendation.recommender.Recommender;

/**
 * 评分任务
 * 
 * @author Birdy
 *
 */
public class RatingTask extends AbstractTask {

	public RatingTask(Class<? extends Recommender> clazz, Configuration configuration) {
		super(clazz, configuration);
	}

	@Override
	protected Collection<Evaluator> getEvaluators(SparseMatrix featureMatrix) {
		Collection<Evaluator> evaluators = new LinkedList<>();
		evaluators.add(new MAEEvaluator());
		evaluators.add(new MPEEvaluator(0.01F));
		evaluators.add(new MSEEvaluator());
		return evaluators;
	}

	@Override
	protected Collection<Float> check(int userIndex) {
		int from = testPaginations[userIndex], to = testPaginations[userIndex + 1];
		List<Float> scoreList = new ArrayList<>(to - from);
		for (int index = from, size = to; index < size; index++) {
			int position = testPositions[index];
			scoreList.add(testMarker.getMark(position));
		}
		return scoreList;
	}

	@Override
	protected List<KeyValue<Integer, Float>> recommend(Recommender recommender, int userIndex) {
		int from = testPaginations[userIndex], to = testPaginations[userIndex + 1];
		int[] discreteFeatures = new int[testMarker.getDiscreteOrder()];
		float[] continuousFeatures = new float[testMarker.getContinuousOrder()];
		List<KeyValue<Integer, Float>> recommendList = new ArrayList<>(to - from);
		for (int index = from, size = to; index < size; index++) {
			int position = testPositions[index];
			for (int dimension = 0; dimension < testMarker.getDiscreteOrder(); dimension++) {
				discreteFeatures[dimension] = testMarker.getDiscreteFeature(dimension, position);
			}
			for (int dimension = 0; dimension < testMarker.getContinuousOrder(); dimension++) {
				continuousFeatures[dimension] = testMarker.getContinuousFeature(dimension, position);
			}
			recommendList.add(new KeyValue<>(discreteFeatures[itemDimension], recommender.predict(discreteFeatures, continuousFeatures)));
		}
		return recommendList;
	}

}
