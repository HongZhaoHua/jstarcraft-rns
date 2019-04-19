package com.jstarcraft.recommendation.recommender.benchmark.ranking;

import com.jstarcraft.ai.modem.ModemDefinition;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.InstanceAccessor;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;
import com.jstarcraft.recommendation.recommender.AbstractRecommender;

/**
 * 
 * Most Popular推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
@ModemDefinition(value = { "itemDimension", "populars" })
public class MostPopularRecommender extends AbstractRecommender {

	private int[] populars;

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, InstanceAccessor model, DataSpace space) {
		super.prepare(configuration, marker, model, space);
		populars = new int[numberOfItems];
	}

	@Override
	protected void doPractice() {
		for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
			populars[itemIndex] = trainMatrix.getColumnScope(itemIndex);
		}
	}

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		int itemIndex = dicreteFeatures[itemDimension];
		return populars[itemIndex];
	}

}
