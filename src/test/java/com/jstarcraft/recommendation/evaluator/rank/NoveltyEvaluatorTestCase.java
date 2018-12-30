package com.jstarcraft.recommendation.evaluator.rank;

import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.recommendation.evaluator.AbstractRankingEvaluatorTestCase;
import com.jstarcraft.recommendation.evaluator.Evaluator;
import com.jstarcraft.recommendation.evaluator.ranking.NoveltyEvaluator;

public class NoveltyEvaluatorTestCase extends AbstractRankingEvaluatorTestCase {

	@Override
	protected Evaluator<?> getEvaluator(SparseMatrix featureMatrix) {
		return new NoveltyEvaluator(10, featureMatrix);
	}

	@Override
	protected float getMeasure() {
		return 16.829857137294585F;
	}

}