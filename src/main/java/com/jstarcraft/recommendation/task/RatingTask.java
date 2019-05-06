package com.jstarcraft.recommendation.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.utility.Int2FloatKeyValue;
import com.jstarcraft.recommendation.configurator.Configuration;
import com.jstarcraft.recommendation.evaluator.Evaluator;
import com.jstarcraft.recommendation.evaluator.rating.MAEEvaluator;
import com.jstarcraft.recommendation.evaluator.rating.MPEEvaluator;
import com.jstarcraft.recommendation.evaluator.rating.MSEEvaluator;
import com.jstarcraft.recommendation.recommender.Recommender;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatCollection;
import it.unimi.dsi.fastutil.floats.FloatList;

/**
 * 评分任务
 * 
 * @author Birdy
 *
 */
public class RatingTask extends AbstractTask<FloatCollection> {

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
	protected FloatCollection check(int userIndex) {
	    DataInstance instance = testMarker.getInstance(0);
		int from = testPaginations[userIndex], to = testPaginations[userIndex + 1];
		FloatList scoreList = new FloatArrayList(to - from);
		for (int index = from, size = to; index < size; index++) {
			int position = testPositions[index];
			instance.setCursor(position);
			scoreList.add(instance.getQuantityMark());
		}
		return scoreList;
	}

	@Override
	protected List<Int2FloatKeyValue> recommend(Recommender recommender, int userIndex) {
	    DataInstance instance = testMarker.getInstance(0);
		int from = testPaginations[userIndex], to = testPaginations[userIndex + 1];
		int[] discreteFeatures = new int[testMarker.getQualityOrder()];
		float[] continuousFeatures = new float[testMarker.getQuantityOrder()];
		List<Int2FloatKeyValue> recommendList = new ArrayList<>(to - from);
		for (int index = from, size = to; index < size; index++) {
			int position = testPositions[index];
			instance.setCursor(position);
			for (int dimension = 0; dimension < testMarker.getQualityOrder(); dimension++) {
				discreteFeatures[dimension] = instance.getQualityFeature(dimension);
			}
			for (int dimension = 0; dimension < testMarker.getQuantityOrder(); dimension++) {
				continuousFeatures[dimension] = instance.getQuantityFeature(dimension);
			}
			recommendList.add(new Int2FloatKeyValue(discreteFeatures[itemDimension], recommender.predict(discreteFeatures, continuousFeatures)));
		}
		return recommendList;
	}

}
