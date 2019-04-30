package com.jstarcraft.recommendation.evaluator.rate;

import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.recommendation.evaluator.AbstractRatingEvaluatorTestCase;
import com.jstarcraft.recommendation.evaluator.Evaluator;
import com.jstarcraft.recommendation.evaluator.rating.MPEEvaluator;

import it.unimi.dsi.fastutil.floats.FloatCollection;

public class MPEEvaluatorTestCase extends AbstractRatingEvaluatorTestCase {

	@Override
	protected Evaluator<FloatCollection> getEvaluator(SparseMatrix featureMatrix) {
		return new MPEEvaluator(0.01F);
	}

	@Override
	protected float getMeasure() {
		return 0.993368700265252F;
	}

}