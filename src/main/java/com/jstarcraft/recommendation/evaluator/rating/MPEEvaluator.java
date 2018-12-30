package com.jstarcraft.recommendation.evaluator.rating;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.recommendation.evaluator.RatingEvaluator;

/**
 * 平均相对误差评估器
 * 
 * <pre>
 * MPE = Mean Prediction  Error
 * </pre>
 *
 * @author Birdy
 */
public class MPEEvaluator extends RatingEvaluator {

	private float mpe;

	public MPEEvaluator(float mpe) {
		this.mpe = mpe;
	}

	@Override
	protected float measure(Collection<Float> checkCollection, List<KeyValue<Integer, Float>> recommendList) {
		float value = 0F;
		Iterator<Float> iterator = checkCollection.iterator();
		for (KeyValue<Integer, Float> keyValue : recommendList) {
			float score = iterator.next();
			float estimate = keyValue.getValue();
			if (Math.abs(score - estimate) > mpe) {
				value++;
			}
		}
		return value;
	}

}
