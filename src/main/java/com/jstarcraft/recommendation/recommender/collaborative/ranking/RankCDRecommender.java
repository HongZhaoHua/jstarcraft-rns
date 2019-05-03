package com.jstarcraft.recommendation.recommender.collaborative.ranking;

import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.DenseModule;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;
import com.jstarcraft.recommendation.recommender.MatrixFactorizationRecommender;

/**
 * 
 * Rank CD推荐器
 * 
 * <pre>
 * </pre>
 * 
 * @author Birdy
 *
 */
public class RankCDRecommender extends MatrixFactorizationRecommender {

	// private float alpha;
	// item confidence

	private float confidence;

	private SparseMatrix weightMatrix;

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, DenseModule model, DataSpace space) {
		super.prepare(configuration, marker, model, space);

		// TODO 此处代码可以消除(使用常量Marker代替或者使用binarize.threshold)
		for (MatrixScalar term : trainMatrix) {
			term.setValue(1F);
		}

		confidence = configuration.getFloat("rec.rankcd.alpha");
		weightMatrix = SparseMatrix.copyOf(trainMatrix, false);
		weightMatrix.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(1F + confidence * scalar.getValue());
		});
	}

	@Override
	protected void doPractice() {
		// Init caches
		double[] userScores = new double[numberOfUsers];
		double[] itemScores = new double[numberOfItems];
		double[] userConfidences = new double[numberOfUsers];
		double[] itemConfidences = new double[numberOfItems];

		// Init Sq
		DenseMatrix itemDeltas = DenseMatrix.valueOf(numberOfFactors, numberOfFactors);
		// Init Sp
		DenseMatrix userDeltas = DenseMatrix.valueOf(numberOfFactors, numberOfFactors);

		for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
			itemDeltas.dotProduct(itemFactors, true, itemFactors, false, MathCalculator.SERIAL);
			// Step 1: update user factors;
			for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
				SparseVector userVector = weightMatrix.getRowVector(userIndex);
				for (VectorScalar term : userVector) {
					int itemIndex = term.getIndex();
					itemScores[itemIndex] = predict(userIndex, itemIndex);
					itemConfidences[itemIndex] = term.getValue();
				}
				for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
					float numerator = 0F, denominator = userRegularization + itemDeltas.getValue(factorIndex, factorIndex);
					// TODO 此处可以改为减法
					for (int k = 0; k < numberOfFactors; k++) {
						if (factorIndex != k) {
							numerator -= userFactors.getValue(userIndex, k) * itemDeltas.getValue(factorIndex, k);
						}
					}
					for (VectorScalar term : userVector) {
						int itemIndex = term.getIndex();
						itemScores[itemIndex] -= userFactors.getValue(userIndex, factorIndex) * itemFactors.getValue(itemIndex, factorIndex);
						numerator += (itemConfidences[itemIndex] - (itemConfidences[itemIndex] - 1) * itemScores[itemIndex]) * itemFactors.getValue(itemIndex, factorIndex);
						denominator += (itemConfidences[itemIndex] - 1) * itemFactors.getValue(itemIndex, factorIndex) * itemFactors.getValue(itemIndex, factorIndex);
					}
					// update puf
					userFactors.setValue(userIndex, factorIndex, numerator / denominator);
					for (VectorScalar term : userVector) {
						int itemIndex = term.getIndex();
						itemScores[itemIndex] += userFactors.getValue(userIndex, factorIndex) * itemFactors.getValue(itemIndex, factorIndex);
					}
				}
			}

			// Update the Sp cache
			userDeltas.dotProduct(userFactors, true, userFactors, false, MathCalculator.SERIAL);
			// Step 2: update item factors;
			for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
				SparseVector itemVector = weightMatrix.getColumnVector(itemIndex);
				for (VectorScalar term : itemVector) {
					int userIndex = term.getIndex();
					userScores[userIndex] = predict(userIndex, itemIndex);
					userConfidences[userIndex] = term.getValue();
				}
				for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
					float numerator = 0F, denominator = itemRegularization + userDeltas.getValue(factorIndex, factorIndex);
					// TODO 此处可以改为减法
					for (int k = 0; k < numberOfFactors; k++) {
						if (factorIndex != k) {
							numerator -= itemFactors.getValue(itemIndex, k) * userDeltas.getValue(k, factorIndex);
						}
					}
					for (VectorScalar term : itemVector) {
						int userIndex = term.getIndex();
						userScores[userIndex] -= userFactors.getValue(userIndex, factorIndex) * itemFactors.getValue(itemIndex, factorIndex);
						numerator += (userConfidences[userIndex] - (userConfidences[userIndex] - 1) * userScores[userIndex]) * userFactors.getValue(userIndex, factorIndex);
						denominator += (userConfidences[userIndex] - 1) * userFactors.getValue(userIndex, factorIndex) * userFactors.getValue(userIndex, factorIndex);
					}
					// update qif
					itemFactors.setValue(itemIndex, factorIndex, numerator / denominator);
					for (VectorScalar term : itemVector) {
						int userIndex = term.getIndex();
						userScores[userIndex] += userFactors.getValue(userIndex, factorIndex) * itemFactors.getValue(itemIndex, factorIndex);
					}
				}
			}
			if (isConverged(iterationStep) && isConverged) {
				break;
			}
			currentLoss = totalLoss;
			// TODO 目前没有totalLoss.
		}
	}

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		DefaultScalar scalar = DefaultScalar.getInstance();
		int userIndex = dicreteFeatures[userDimension];
		int itemIndex = dicreteFeatures[itemDimension];
		return scalar.dotProduct(userFactors.getRowVector(userIndex), itemFactors.getRowVector(itemIndex)).getValue();
	}

}
