package com.jstarcraft.recommendation.evaluator;

import java.util.List;

import com.jstarcraft.ai.utility.Int2FloatKeyValue;

/**
 * 抽象评估器
 * 
 * @author Birdy
 *
 */
public abstract class AbstractEvaluator<T> implements Evaluator<T> {

	/**
	 * 统计列表
	 * 
	 * @param checkCollection
	 * @param recommendList
	 * @return
	 */
	protected abstract int count(T checkCollection, List<Int2FloatKeyValue> recommendList);

	/**
	 * 测量列表
	 * 
	 * @param checkCollection
	 * @param recommendList
	 * @return
	 */
	protected abstract float measure(T checkCollection, List<Int2FloatKeyValue> recommendList);

	@Override
	public final Int2FloatKeyValue evaluate(T checkCollection, List<Int2FloatKeyValue> recommendList) {
		return new Int2FloatKeyValue(count(checkCollection, recommendList), measure(checkCollection, recommendList));
	}

}
