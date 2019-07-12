package com.jstarcraft.ai.evaluate;

import java.util.List;

import com.jstarcraft.ai.utility.Integer2FloatKeyValue;

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
	protected abstract int count(T checkCollection, List<Integer2FloatKeyValue> recommendList);

	/**
	 * 测量列表
	 * 
	 * @param checkCollection
	 * @param recommendList
	 * @return
	 */
	protected abstract float measure(T checkCollection, List<Integer2FloatKeyValue> recommendList);

	@Override
	public final Integer2FloatKeyValue evaluate(T checkCollection, List<Integer2FloatKeyValue> recommendList) {
		return new Integer2FloatKeyValue(count(checkCollection, recommendList), measure(checkCollection, recommendList));
	}

}
