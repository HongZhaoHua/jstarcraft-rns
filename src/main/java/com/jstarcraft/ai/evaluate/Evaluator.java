package com.jstarcraft.ai.evaluate;

import java.util.List;

import com.jstarcraft.ai.utility.Integer2FloatKeyValue;

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
	Integer2FloatKeyValue evaluate(T checkCollection, List<Integer2FloatKeyValue> recommendList);

}
