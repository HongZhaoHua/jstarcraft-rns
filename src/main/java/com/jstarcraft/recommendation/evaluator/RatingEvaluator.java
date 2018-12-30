package com.jstarcraft.recommendation.evaluator;

import java.util.Collection;
import java.util.List;

import com.jstarcraft.core.utility.KeyValue;

/**
 * 面向评分预测的评估器
 * 
 * @author Birdy
 *
 */
public abstract class RatingEvaluator extends AbstractEvaluator<Float> {

	@Override
	protected int count(Collection<Float> checkCollection, List<KeyValue<Integer, Float>> recommendList) {
		return checkCollection.size();
	}

}
