package com.jstarcraft.rns.evaluate.rate;

import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.rns.evaluate.AbstractRatingEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.Evaluator;
import com.jstarcraft.rns.evaluate.rating.MAEEvaluator;

import it.unimi.dsi.fastutil.floats.FloatCollection;

public class MAEEvaluatorTestCase extends AbstractRatingEvaluatorTestCase {

	@Override
	protected Evaluator<FloatCollection> getEvaluator(SparseMatrix featureMatrix) {
		return new MAEEvaluator();
	}

	@Override
	protected float getMeasure() {
		return 546.6342838196286F;
	}

}