package com.jstarcraft.recommendation.evaluator.rank;

import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.recommendation.evaluator.AbstractRankingEvaluatorTestCase;
import com.jstarcraft.recommendation.evaluator.Evaluator;
import com.jstarcraft.recommendation.evaluator.ranking.AUCEvaluator;

public class AUCEvaluatorTestCase extends AbstractRankingEvaluatorTestCase {

	@Override
	protected Evaluator<?> getEvaluator(SparseMatrix featureMatrix) {
		return new AUCEvaluator(10);
	}

	@Override
	protected float getMeasure() {
		return 0.83753127F;
	}

}
