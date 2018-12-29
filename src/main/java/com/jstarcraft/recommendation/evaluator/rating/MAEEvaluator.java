package com.jstarcraft.recommendation.evaluator.rating;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.recommendation.evaluator.RatingEvaluator;

/**
 * 平均绝对误差评估器
 * 
 * <pre>
 * MAE = Mean Absolute Error
 * </pre>
 *
 * @author Birdy
 */
public class MAEEvaluator extends RatingEvaluator {

	@Override
	protected float measure(Collection<Float> checkCollection, List<KeyValue<Integer, Float>> recommendList) {
		float value = 0F;
		Iterator<Float> iterator = checkCollection.iterator();
		for (KeyValue<Integer, Float> keyValue : recommendList) {
			float score = iterator.next();
			float estimate = keyValue.getValue();
			value += Math.abs(score - estimate);
		}
		return value;
	}

}
