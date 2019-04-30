package com.jstarcraft.recommendation.evaluator;

import java.util.Collection;
import java.util.List;

import com.jstarcraft.ai.utility.Int2FloatKeyValue;

/**
 * 面向评分预测的评估器
 * 
 * @author Birdy
 *
 */
public abstract class RatingEvaluator extends AbstractEvaluator<Float> {

	@Override
	protected int count(Collection<Float> checkCollection, List<Int2FloatKeyValue> recommendList) {
		return checkCollection.size();
	}

}
