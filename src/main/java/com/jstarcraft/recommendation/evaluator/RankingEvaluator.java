package com.jstarcraft.recommendation.evaluator;

import java.util.Collection;
import java.util.List;

import com.jstarcraft.core.utility.KeyValue;

/**
 * 面向排名预测的评估器
 * 
 * @author Birdy
 *
 */
public abstract class RankingEvaluator extends AbstractEvaluator<Integer> {

	/** 大小 */
	protected int size;

	protected RankingEvaluator(int size) {
		this.size = size;
	}

	@Override
	protected int count(Collection<Integer> checkCollection, List<KeyValue<Integer, Float>> recommendList) {
		return 1;
	}

}
