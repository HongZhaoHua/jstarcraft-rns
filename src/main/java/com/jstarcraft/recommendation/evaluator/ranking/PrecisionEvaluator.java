package com.jstarcraft.recommendation.evaluator.ranking;

import java.util.Collection;
import java.util.List;

import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.recommendation.evaluator.RankingEvaluator;

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
	protected float measure(Collection<Integer> checkCollection, List<KeyValue<Integer, Float>> recommendList) {
		if (recommendList.size() > size) {
			recommendList = recommendList.subList(0, size);
		}
		int count = 0;
		for (KeyValue<Integer, Float> keyValue : recommendList) {
			if (checkCollection.contains(keyValue.getKey())) {
				count++;
			}
		}
		return count / (size + 0F);
	}

}
