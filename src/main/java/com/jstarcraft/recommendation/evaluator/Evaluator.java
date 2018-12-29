package com.jstarcraft.recommendation.evaluator;

import java.util.Collection;
import java.util.List;

import com.jstarcraft.core.utility.KeyValue;

/**
 * 评估器
 * 
 * @author Birdy
 *
 */
public interface Evaluator<T> {

	/**
	 * 评估
	 * 
	 * @param checkCollection
	 * @param recommendList
	 * @return
	 */
	KeyValue<Integer, Float> evaluate(Collection<T> checkCollection, List<KeyValue<Integer, Float>> recommendList);

}
