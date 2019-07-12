package com.jstarcraft.rns.evaluate.rating;

import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.rns.evaluate.AbstractRatingEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.Evaluator;
import com.jstarcraft.rns.evaluate.rating.MPEEvaluator;

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