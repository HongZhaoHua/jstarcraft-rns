package com.jstarcraft.recommendation.recommender.collaborative.rating;

import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.recommendation.recommender.MatrixFactorizationRecommender;
import com.jstarcraft.recommendation.utility.MatrixUtility;

/**
 * 
 * MF ALS推荐器
 * 
 * <pre>
 * Large-Scale Parallel Collaborative Filtering for the Netflix Prize.
 * http://www.hpl.hp.com/personal/Robert_Schreiber/papers/2008%20AAIM%20Netflix/
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class MFALSRecommender extends MatrixFactorizationRecommender {

	@Override
	protected void doPractice() {
		DenseVector scoreVector = DenseVector.valueOf(numberOfFactors);
		DenseMatrix inverseMatrix = DenseMatrix.valueOf(numberOfFactors, numberOfFactors);
		DenseMatrix transposeMatrix = DenseMatrix.valueOf(numberOfFactors, numberOfFactors);
		DenseMatrix copyMatrix = DenseMatrix.valueOf(numberOfFactors, numberOfFactors);
		// TODO 可以考虑只获取有评分的用户?
		for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
			// fix item matrix M, solve user matrix U
			for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
				// number of items rated by user userIdx
				SparseVector userVector = trainMatrix.getRowVector(userIndex);
				int size = userVector.getElementSize();
				if (size == 0) {
					continue;
				}
				// TODO 此处应该避免valueOf
				DenseMatrix rateMatrix = DenseMatrix.valueOf(size, numberOfFactors);
				DenseVector rateVector = DenseVector.valueOf(size);
				int index = 0;
				for (VectorScalar term : userVector) {
					// step 1:
					int itemIndex = term.getIndex();
					rateMatrix.getRowVector(index).copyVector(itemFactors.getRowVector(itemIndex));

					// step 2:
					// ratings of this userIdx
					rateVector.setValue(index++, term.getValue());
				}

				// step 3: the updated user matrix wrt user j
				DenseMatrix matrix = transposeMatrix;
				matrix.dotProduct(rateMatrix, true, rateMatrix, false, MathCalculator.SERIAL);
				for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
					matrix.shiftValue(factorIndex, factorIndex, userRegularization * size);
				}
				scoreVector.dotProduct(rateMatrix, true, rateVector, MathCalculator.SERIAL);
				userFactors.getRowVector(userIndex).dotProduct(MatrixUtility.inverse(matrix, copyMatrix, inverseMatrix), false, scoreVector, MathCalculator.SERIAL);
			}

			// TODO 可以考虑只获取有评分的条目?
			// fix user matrix U, solve item matrix M
			for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
				// latent factor of users that have rated item itemIdx
				// number of users rate item j
				SparseVector itemVector = trainMatrix.getColumnVector(itemIndex);
				int size = itemVector.getElementSize();
				if (size == 0) {
					continue;
				}

				// TODO 此处应该避免valueOf
				DenseMatrix rateMatrix = DenseMatrix.valueOf(size, numberOfFactors);
				DenseVector rateVector = DenseVector.valueOf(size);
				int index = 0;
				for (VectorScalar term : itemVector) {
					// step 1:
					int userIndex = term.getIndex();
					rateMatrix.getRowVector(index).copyVector(userFactors.getRowVector(userIndex));

					// step 2:
					// ratings of this item
					rateVector.setValue(index++, term.getValue());
				}

				// step 3: the updated item matrix wrt item j
				DenseMatrix matrix = transposeMatrix;
				matrix.dotProduct(rateMatrix, true, rateMatrix, false, MathCalculator.SERIAL);
				for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
					matrix.shiftValue(factorIndex, factorIndex, itemRegularization * size);
				}
				scoreVector.dotProduct(rateMatrix, true, rateVector, MathCalculator.SERIAL);
				itemFactors.getRowVector(itemIndex).dotProduct(MatrixUtility.inverse(matrix, copyMatrix, inverseMatrix), false, scoreVector, MathCalculator.SERIAL);
			}
		}
	}

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		int userIndex = dicreteFeatures[userDimension];
		int itemIndex = dicreteFeatures[itemDimension];
		float value = super.predict(userIndex, itemIndex);
		if (value > maximumOfScore) {
			value = maximumOfScore;
		} else if (value < minimumOfScore) {
			value = minimumOfScore;
		}
		return value;
	}

}
