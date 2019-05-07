package com.jstarcraft.rns.evaluator.rank;

import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.rns.evaluator.AbstractRankingEvaluatorTestCase;
import com.jstarcraft.rns.evaluator.Evaluator;
import com.jstarcraft.rns.evaluator.ranking.PrecisionEvaluator;

import it.unimi.dsi.fastutil.ints.IntCollection;

public class PrecisionEvaluatorTestCase extends AbstractRankingEvaluatorTestCase {

	@Override
	protected Evaluator<IntCollection> getEvaluator(SparseMatrix featureMatrix) {
		return new PrecisionEvaluator(10);
	}

	@Override
	protected float getMeasure() {
		return 0.06743967F;
	}

}