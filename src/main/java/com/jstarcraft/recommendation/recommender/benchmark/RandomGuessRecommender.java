package com.jstarcraft.recommendation.recommender.benchmark;

import com.jstarcraft.ai.modem.ModemDefinition;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.recommendation.recommender.AbstractRecommender;

/**
 * 
 * Random Guess推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
@ModemDefinition(value = { "userDimension", "itemDimension", "numberOfItems", "minimumOfScore", "maximumOfScore" })
public class RandomGuessRecommender extends AbstractRecommender {

	@Override
	protected void doPractice() {
	}

	@Override
	public synchronized float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		int userIndex = dicreteFeatures[userDimension];
		int itemIndex = dicreteFeatures[itemDimension];
		RandomUtility.setSeed(userIndex * numberOfItems + itemIndex);
		return RandomUtility.randomFloat(minimumOfScore, maximumOfScore);
	}

}
