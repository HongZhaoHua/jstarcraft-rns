package com.jstarcraft.recommendation.recommender.collaborative.ranking;

import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.DenseModule;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;
import com.jstarcraft.recommendation.recommender.MatrixFactorizationRecommender;
import com.jstarcraft.recommendation.utility.LogisticUtility;

/**
 * 
 * ListwiseMF推荐器
 * 
 * <pre>
 * List-wise learning to rank with matrix factorization for collaborative filtering
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class ListwiseMFRecommender extends MatrixFactorizationRecommender {

	private DenseVector userExponentials;

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, DenseModule model, DataSpace space) {
		super.prepare(configuration, marker, model, space);
		userExponentials = DenseVector.valueOf(numberOfUsers);
		for (MatrixScalar matrixentry : trainMatrix) {
			int userIndex = matrixentry.getRow();
			float score = matrixentry.getValue();
			userExponentials.shiftValue(userIndex, (float) Math.exp(score));
		}
	}

	@Override
	protected void doPractice() {
		for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
			totalLoss = 0F;
			for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
				SparseVector userVector = trainMatrix.getRowVector(userIndex);
				if (userVector.getElementSize() == 0) {
					continue;
				}
				float exponential = 0F;
				for (VectorScalar term : userVector) {
					exponential += Math.exp(predict(userIndex, term.getIndex()));
				}
				for (VectorScalar term : userVector) {
					int itemIndex = term.getIndex();
					float score = term.getValue();
					float predict = predict(userIndex, itemIndex);
					float error = (float) (Math.exp(score) / userExponentials.getValue(userIndex) - Math.log(Math.exp(predict) / exponential)) * LogisticUtility.getGradient(predict);
					totalLoss -= error;
					// update factors
					for (int factorIdx = 0; factorIdx < numberOfFactors; factorIdx++) {
						float userFactor = userFactors.getValue(userIndex, factorIdx);
						float itemFactor = itemFactors.getValue(itemIndex, factorIdx);
						float userDelta = error * itemFactor - userRegularization * userFactor;
						float itemDelta = error * userFactor - itemRegularization * itemFactor;
						userFactors.shiftValue(userIndex, factorIdx, learnRate * userDelta);
						itemFactors.shiftValue(itemIndex, factorIdx, learnRate * itemDelta);
						totalLoss += 0.5D * userRegularization * userFactor * userFactor + 0.5D * itemRegularization * itemFactor * itemFactor;
					}
				}
			}

			if (isConverged(iterationStep) && isConverged) {
				break;
			}
			isLearned(iterationStep);
			currentLoss = totalLoss;
		}
	}

}