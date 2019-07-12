package com.jstarcraft.rns.evaluate.ranking;

import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.rns.evaluate.AbstractRankingEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.Evaluator;
import com.jstarcraft.rns.evaluate.ranking.AUCEvaluator;

import it.unimi.dsi.fastutil.ints.IntCollection;

public class AUCEvaluatorTestCase extends AbstractRankingEvaluatorTestCase {

	@Override
	protected Evaluator<IntCollection> getEvaluator(SparseMatrix featureMatrix) {
		return new AUCEvaluator(10);
	}

	@Override
	protected float getMeasure() {
		return 0.83753127F;
	}

}
