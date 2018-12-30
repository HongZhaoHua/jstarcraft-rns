package com.jstarcraft.recommendation.evaluator;

import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.InstanceAccessor;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;
import com.jstarcraft.recommendation.recommender.Recommender;

public class MockRecommender implements Recommender {

	private int itemDimension;

	private SparseMatrix matrix;

	MockRecommender(int itemDimension, SparseMatrix matrix) {
		this.itemDimension = itemDimension;
		this.matrix = matrix;
	}

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, InstanceAccessor model, DataSpace space) {
	}

	@Override
	public void practice() {
	}

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		return matrix.getColumnScope(dicreteFeatures[itemDimension]);
	}

}
