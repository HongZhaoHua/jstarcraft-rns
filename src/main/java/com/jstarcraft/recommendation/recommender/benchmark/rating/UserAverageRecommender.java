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
 * User Average推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
@ModelDefinition(value = { "userDimension", "userMeans" })
public class UserAverageRecommender extends AbstractRecommender {

	/** 用户平均分数 */
	private float[] userMeans;

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, InstanceAccessor model, DataSpace space) {
		super.prepare(configuration, marker, model, space);
		userMeans = new float[numberOfUsers];
	}

	@Override
	protected void doPractice() {
		for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
			SparseVector userVector = trainMatrix.getRowVector(userIndex);
			userMeans[userIndex] = userVector.getElementSize() == 0 ? meanOfScore : userVector.getSum(false) / userVector.getElementSize();
		}
	}

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		int userIndex = dicreteFeatures[userDimension];
		return userMeans[userIndex];
	}

}
