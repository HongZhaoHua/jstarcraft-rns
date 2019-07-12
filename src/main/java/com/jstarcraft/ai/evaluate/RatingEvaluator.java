package com.jstarcraft.ai.evaluate;

import java.util.List;

import com.jstarcraft.ai.utility.Integer2FloatKeyValue;

import it.unimi.dsi.fastutil.floats.FloatCollection;

/**
 * 面向评分预测的评估器
 * 
 * @author Birdy
 *
 */
public abstract class RatingEvaluator extends AbstractEvaluator<FloatCollection> {

	@Override
	protected int count(FloatCollection checkCollection, List<Integer2FloatKeyValue> recommendList) {
		return checkCollection.size();
	}

}
