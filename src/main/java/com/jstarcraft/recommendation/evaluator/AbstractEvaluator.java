package com.jstarcraft.recommendation.evaluator;

import java.util.Collection;
import java.util.List;

import com.jstarcraft.core.utility.KeyValue;

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
	protected abstract int count(Collection<T> checkCollection, List<KeyValue<Integer, Float>> recommendList);

	/**
	 * 测量列表
	 * 
	 * @param checkCollection
	 * @param recommendList
	 * @return
	 */
	protected abstract float measure(Collection<T> checkCollection, List<KeyValue<Integer, Float>> recommendList);

	@Override
	public final KeyValue<Integer, Float> evaluate(Collection<T> checkCollection, List<KeyValue<Integer, Float>> recommendList) {
		return new KeyValue<>(count(checkCollection, recommendList), measure(checkCollection, recommendList));
	}

}
