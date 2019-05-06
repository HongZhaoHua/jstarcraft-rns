package com.jstarcraft.recommendation.recommender.collaborative.rating;

import java.util.Map.Entry;

import com.jstarcraft.ai.data.DataInstance;
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
public class BUCMRatingRecommender extends BUCMRecommender {

	@Override
	public float predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
		float value = 0F, probabilities = 0F;
		for (Entry<Float, Integer> term : scoreIndexes.entrySet()) {
			float rate = term.getKey();
			float probability = 0F;
			for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
				probability += userTopicProbabilities.getValue(userIndex, topicIndex) * topicItemProbabilities.getValue(topicIndex, itemIndex) * topicItemRateProbabilities[topicIndex][itemIndex][term.getValue()];
			}
			value += probability * rate;
			probabilities += probability;
		}
		return value / probabilities;
	}

}
