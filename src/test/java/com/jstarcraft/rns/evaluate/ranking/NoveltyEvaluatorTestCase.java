package com.jstarcraft.rns.evaluate.ranking;

import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.rns.evaluate.AbstractRankingEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.Evaluator;
import com.jstarcraft.rns.evaluate.ranking.NoveltyEvaluator;

import it.unimi.dsi.fastutil.ints.IntCollection;

public class NoveltyEvaluatorTestCase extends AbstractRankingEvaluatorTestCase {

	@Override
	protected Evaluator<IntCollection> getEvaluator(SparseMatrix featureMatrix) {
		return new NoveltyEvaluator(10, featureMatrix);
	}

	@Override
	protected float getMeasure() {
		return 16.829857137294585F;
	}

}