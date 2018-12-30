package com.jstarcraft.recommendation.recommender.benchmark.rating;

import com.jstarcraft.ai.model.ModelDefinition;
import com.jstarcraft.recommendation.recommender.AbstractRecommender;

/**
 * 
 * Global Average推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
@ModelDefinition(value = { "meanOfScore" })
public class GlobalAverageRecommender extends AbstractRecommender {

	@Override
	protected void doPractice() {
	}

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		return meanOfScore;
	}

}
