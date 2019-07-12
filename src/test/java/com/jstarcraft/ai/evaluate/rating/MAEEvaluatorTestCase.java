package com.jstarcraft.ai.evaluate.rating;

import com.jstarcraft.ai.evaluate.AbstractRatingEvaluatorTestCase;
import com.jstarcraft.ai.evaluate.Evaluator;
import com.jstarcraft.ai.evaluate.rating.MAEEvaluator;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;

import it.unimi.dsi.fastutil.floats.FloatCollection;

public class MAEEvaluatorTestCase extends AbstractRatingEvaluatorTestCase {

    @Override
    protected Evaluator<FloatCollection> getEvaluator(SparseMatrix featureMatrix) {
        return new MAEEvaluator();
    }

    @Override
    protected float getMeasure() {
        return 0.12530328F;
    }

}