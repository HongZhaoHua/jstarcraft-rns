package com.jstarcraft.ai.evaluate;

import java.util.List;

import com.jstarcraft.ai.utility.Integer2FloatKeyValue;

import it.unimi.dsi.fastutil.ints.IntCollection;

/**
 * 面向排名预测的评估器
 * 
 * @author Birdy
 *
 */
public abstract class RankingEvaluator extends AbstractEvaluator<IntCollection> {

	/** 大小 */
	protected int size;

	protected RankingEvaluator(int size) {
		this.size = size;
	}

	@Override
	protected int count(IntCollection checkCollection, List<Integer2FloatKeyValue> recommendList) {
		return 1;
	}

}
