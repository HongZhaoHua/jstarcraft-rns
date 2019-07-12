package com.jstarcraft.ai.evaluate.ranking;

import java.util.ArrayList;
import java.util.List;

import com.jstarcraft.ai.evaluate.RankingEvaluator;
import com.jstarcraft.ai.utility.Integer2FloatKeyValue;
import com.jstarcraft.ai.utility.MathUtility;

import it.unimi.dsi.fastutil.ints.IntCollection;

/**
 * <pre>
 * NDCG = Normalized Discounted Cumulative Gain
 * https://en.wikipedia.org/wiki/Discounted_cumulative_gain
 * </pre>
 *
 * @author Birdy
 */
public class NDCGEvaluator extends RankingEvaluator {

	private List<Float> idcgs;

	public NDCGEvaluator(int size) {
		super(size);
		idcgs = new ArrayList<>(size + 1);
		idcgs.add(0F);
		for (int index = 0; index < size; index++) {
			idcgs.add((float) (1F / MathUtility.logarithm(index + 2F, 2) + idcgs.get(index)));
		}
	}

	@Override
	protected float measure(IntCollection checkCollection, List<Integer2FloatKeyValue> recommendList) {
		if (recommendList.size() > size) {
			recommendList = recommendList.subList(0, size);
		}
		float dcg = 0F;
		// calculate DCG
		int size = recommendList.size();
		for (int index = 0; index < size; index++) {
			int itemIndex = recommendList.get(index).getKey();
			if (!checkCollection.contains(itemIndex)) {
				continue;
			}
			dcg += (float) (1F / MathUtility.logarithm(index + 2F, 2));
		}
		return dcg / idcgs.get(checkCollection.size() < size ? checkCollection.size() : size);
	}

}
