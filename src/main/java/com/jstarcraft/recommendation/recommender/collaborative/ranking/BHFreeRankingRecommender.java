package com.jstarcraft.recommendation.recommender.collaborative.ranking;

import java.util.Map.Entry;

import com.jstarcraft.recommendation.recommender.collaborative.BHFreeRecommender;

/**
 * 
 * BH Free推荐器
 * 
 * <pre>
 * Balancing Prediction and Recommendation Accuracy: Hierarchical Latent Factors for Preference Data
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class BHFreeRankingRecommender extends BHFreeRecommender {

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		int userIndex = dicreteFeatures[userDimension];
		int itemIndex = dicreteFeatures[itemDimension];
		float value = 0F;
		for (Entry<Float, Integer> entry : scoreIndexes.entrySet()) {
			float rate = entry.getKey();
			float probability = 0F;
			for (int userTopic = 0; userTopic < numberOfUserTopics; userTopic++) {
				for (int itemTopic = 0; itemTopic < numberOfItemTopics; itemTopic++) {
					probability += user2TopicProbabilities.getValue(userIndex, userTopic) * userTopic2ItemTopicProbabilities.getValue(userTopic, itemTopic) * userTopic2ItemTopicItemSums[userTopic][itemTopic][itemIndex] * userTopic2ItemTopicRateProbabilities[userTopic][itemTopic][entry.getValue()];
				}
			}
			value += rate * probability;
		}
		return value;
	}
}
