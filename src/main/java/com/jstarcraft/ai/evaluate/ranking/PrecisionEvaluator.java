package com.jstarcraft.ai.evaluate.ranking;

import java.util.List;

import com.jstarcraft.ai.evaluate.RankingEvaluator;
import com.jstarcraft.ai.utility.Integer2FloatKeyValue;

import it.unimi.dsi.fastutil.ints.IntCollection;

/**
 * 精确度评估器
 * 
 * <pre>
 * https://en.wikipedia.org/wiki/Precision_and_recall
 * </pre>
 * 
 * @author Birdy
 */
public class PrecisionEvaluator extends RankingEvaluator {

	public PrecisionEvaluator(int size) {
		super(size);
	}

	@Override
	protected float measure(IntCollection checkCollection, List<Integer2FloatKeyValue> recommendList) {
		if (recommendList.size() > size) {
			recommendList = recommendList.subList(0, size);
		}
		int count = 0;
		for (Integer2FloatKeyValue keyValue : recommendList) {
			if (checkCollection.contains(keyValue.getKey())) {
				count++;
			}
		}
		return count / (size + 0F);
	}

}
