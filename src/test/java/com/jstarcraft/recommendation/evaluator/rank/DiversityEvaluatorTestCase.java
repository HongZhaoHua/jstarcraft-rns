package com.jstarcraft.recommendation.evaluator.rank;

import com.jstarcraft.ai.math.algorithm.similarity.CosineSimilarity;
import com.jstarcraft.ai.math.algorithm.similarity.Similarity;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.matrix.SymmetryMatrix;
import com.jstarcraft.recommendation.evaluator.AbstractRankingEvaluatorTestCase;
import com.jstarcraft.recommendation.evaluator.Evaluator;
import com.jstarcraft.recommendation.evaluator.ranking.DiversityEvaluator;

public class DiversityEvaluatorTestCase extends AbstractRankingEvaluatorTestCase {

	@Override
	protected Evaluator<?> getEvaluator(SparseMatrix featureMatrix) {
		// Item Similarity Matrix
		Similarity similarity = new CosineSimilarity();
		SymmetryMatrix similarityMatrix = similarity.makeSimilarityMatrix(featureMatrix, true, 0F);
		return new DiversityEvaluator(10, similarityMatrix);
	}

	@Override
	protected float getMeasure() {
		return 0.054952923F;
	}

}