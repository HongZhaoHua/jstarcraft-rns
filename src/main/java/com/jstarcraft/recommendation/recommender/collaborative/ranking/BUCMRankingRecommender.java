package com.jstarcraft.recommendation.recommender.collaborative.ranking;

import java.util.Map.Entry;

import com.jstarcraft.recommendation.recommender.collaborative.BUCMRecommender;

/**
 * 
 * BUCM推荐器
 * 
 * <pre>
 * Bayesian User Community Model
 * Modeling Item Selection and Relevance for Accurate Recommendations
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class BUCMRankingRecommender extends BUCMRecommender {

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		int userIndex = dicreteFeatures[userDimension];
		int itemIndex = dicreteFeatures[itemDimension];
		float value = 0F;
		for (int topicIndex = 0; topicIndex < numberOfFactors; ++topicIndex) {
			float sum = 0F;
			for (Entry<Float, Integer> term : scoreIndexes.entrySet()) {
				double rate = term.getKey();
				if (rate > meanOfScore) {
					sum += topicItemRateProbabilities[topicIndex][itemIndex][term.getValue()];
				}
			}
			value += userTopicProbabilities.getValue(userIndex, topicIndex) * topicItemProbabilities.getValue(topicIndex, itemIndex) * sum;
		}
		return value;
	}

}
