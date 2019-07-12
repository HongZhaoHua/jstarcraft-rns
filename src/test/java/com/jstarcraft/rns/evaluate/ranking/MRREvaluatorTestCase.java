package com.jstarcraft.rns.evaluate.ranking;

import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.rns.evaluate.AbstractRankingEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.Evaluator;
import com.jstarcraft.rns.evaluate.ranking.MRREvaluator;

import it.unimi.dsi.fastutil.ints.IntCollection;

public class MRREvaluatorTestCase extends AbstractRankingEvaluatorTestCase {

	@Override
	protected Evaluator<IntCollection> getEvaluator(SparseMatrix featureMatrix) {
		return new MRREvaluator(10);
	}

	@Override
	protected float getMeasure() {
		return 0.3753629F;
	}

}
