package com.jstarcraft.ai.evaluate.ranking;

import java.util.List;

import com.jstarcraft.ai.evaluate.RankingEvaluator;
import com.jstarcraft.ai.utility.Integer2FloatKeyValue;

import it.unimi.dsi.fastutil.ints.IntCollection;

/**
 * 平均倒数排名评估器
 * 
 * <pre>
 * MRR = Mean Reciprocal Rank(平均倒数排名)
 * https://en.wikipedia.org/wiki/Mean_reciprocal_rank
 * </pre>
 *
 * @author Birdy
 */
public class MRREvaluator extends RankingEvaluator {

	public MRREvaluator(int size) {
		super(size);
	}

	
	@Override
	protected float measure(IntCollection checkCollection, List<Integer2FloatKeyValue> recommendList) {
		if (recommendList.size() > size) {
			recommendList = recommendList.subList(0, size);
		}
		int size = recommendList.size();
		for (int index = 0; index < size; index++) {
			int key = recommendList.get(index).getKey();
			if (checkCollection.contains(key)) {
				return 1F / (index + 1);
			}
		}
		return 0F;
	}

}
