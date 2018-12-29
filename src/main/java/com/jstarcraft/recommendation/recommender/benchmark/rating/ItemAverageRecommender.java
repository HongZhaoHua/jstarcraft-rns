package com.jstarcraft.recommendation.recommender.benchmark.rating;

import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.model.ModelDefinition;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.InstanceAccessor;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;
import com.jstarcraft.recommendation.recommender.AbstractRecommender;

/**
 * 
 * Item Average推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
@ModelDefinition(value = { "itemDimension", "itemMeans" })
public class ItemAverageRecommender extends AbstractRecommender {

	/** 物品平均分数 */
	private float[] itemMeans;

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, InstanceAccessor model, DataSpace space) {
		super.prepare(configuration, marker, model, space);
		itemMeans = new float[numberOfItems];
	}

	@Override
	protected void doPractice() {
		for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
			SparseVector itemVector = trainMatrix.getColumnVector(itemIndex);
			itemMeans[itemIndex] = itemVector.getElementSize() == 0 ? meanOfScore : itemVector.getSum(false) / itemVector.getElementSize();
		}
	}

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		int itemIndex = dicreteFeatures[itemDimension];
		return itemMeans[itemIndex];
	}

}
