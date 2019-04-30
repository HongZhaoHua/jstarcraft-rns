package com.jstarcraft.recommendation.evaluator.rating;

import java.util.Iterator;
import java.util.List;

import com.jstarcraft.ai.utility.Int2FloatKeyValue;
import com.jstarcraft.recommendation.evaluator.RatingEvaluator;

import it.unimi.dsi.fastutil.floats.FloatCollection;

/**
 * 均方误差
 * 
 * <pre>
 * MSE = Mean Squared Error
 * </pre>
 *
 * @author Birdy
 */
public class MSEEvaluator extends RatingEvaluator {

	@Override
	protected float measure(FloatCollection checkCollection, List<Int2FloatKeyValue> recommendList) {
		float value = 0F;
		Iterator<Float> iterator = checkCollection.iterator();
		for (Int2FloatKeyValue keyValue : recommendList) {
			double score = iterator.next();
			double estimate = keyValue.getValue();
			value += Math.pow(score - estimate, 2);
		}
		return value;
	}

}
