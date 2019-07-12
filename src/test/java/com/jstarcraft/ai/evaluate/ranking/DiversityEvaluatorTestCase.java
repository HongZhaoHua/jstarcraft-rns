package com.jstarcraft.ai.evaluate.ranking;

import com.jstarcraft.ai.evaluate.AbstractRankingEvaluatorTestCase;
import com.jstarcraft.ai.evaluate.Evaluator;
import com.jstarcraft.ai.evaluate.ranking.DiversityEvaluator;
import com.jstarcraft.ai.math.algorithm.similarity.CosineSimilarity;
import com.jstarcraft.ai.math.algorithm.similarity.Similarity;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.matrix.SymmetryMatrix;

import it.unimi.dsi.fastutil.ints.IntCollection;

public class DiversityEvaluatorTestCase extends AbstractRankingEvaluatorTestCase {

    @Override
    protected Evaluator<IntCollection> getEvaluator(SparseMatrix featureMatrix) {
        // Item Similarity Matrix
        Similarity similarity = new CosineSimilarity();
        SymmetryMatrix similarityMatrix = similarity.makeSimilarityMatrix(featureMatrix, true, 0F);
        return new DiversityEvaluator(10, similarityMatrix);
    }

    @Override
    protected float getMeasure() {
        return 0.24876F;
    }

}