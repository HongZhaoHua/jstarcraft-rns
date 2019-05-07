package com.jstarcraft.rns.evaluator;

import java.util.List;

import com.jstarcraft.ai.utility.Int2FloatKeyValue;

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
	Int2FloatKeyValue evaluate(T checkCollection, List<Int2FloatKeyValue> recommendList);

}
