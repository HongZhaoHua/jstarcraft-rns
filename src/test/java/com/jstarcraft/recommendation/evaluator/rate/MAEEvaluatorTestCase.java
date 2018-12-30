package com.jstarcraft.recommendation.evaluator.rate;

import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.recommendation.evaluator.AbstractRatingEvaluatorTestCase;
import com.jstarcraft.recommendation.evaluator.Evaluator;
import com.jstarcraft.recommendation.evaluator.rating.MAEEvaluator;

public class MAEEvaluatorTestCase extends AbstractRatingEvaluatorTestCase {

	@Override
	protected Evaluator<?> getEvaluator(SparseMatrix featureMatrix) {
		return new MAEEvaluator();
	}

	@Override
	protected float getMeasure() {
		return 546.6342838196286F;
	}

}