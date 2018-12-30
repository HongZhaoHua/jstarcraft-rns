package com.jstarcraft.recommendation.evaluator.rank;

import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.recommendation.evaluator.AbstractRankingEvaluatorTestCase;
import com.jstarcraft.recommendation.evaluator.Evaluator;
import com.jstarcraft.recommendation.evaluator.ranking.NDCGEvaluator;

public class NDCGEvaluatorTestCase extends AbstractRankingEvaluatorTestCase {

	@Override
	protected Evaluator<?> getEvaluator(SparseMatrix featureMatrix) {
		return new NDCGEvaluator(10);
	}

	@Override
	protected float getMeasure() {
		return 0.4448136F;
	}

}
