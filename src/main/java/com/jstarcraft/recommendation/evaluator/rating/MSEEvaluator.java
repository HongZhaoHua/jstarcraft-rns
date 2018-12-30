package com.jstarcraft.recommendation.evaluator.rating;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.recommendation.evaluator.RatingEvaluator;

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
	protected float measure(Collection<Float> checkCollection, List<KeyValue<Integer, Float>> recommendList) {
		float value = 0F;
		Iterator<Float> iterator = checkCollection.iterator();
		for (KeyValue<Integer, Float> keyValue : recommendList) {
			double score = iterator.next();
			double estimate = keyValue.getValue();
			value += Math.pow(score - estimate, 2);
		}
		return value;
	}

}
