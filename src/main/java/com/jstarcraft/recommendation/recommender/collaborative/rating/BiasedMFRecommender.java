package com.jstarcraft.recommendation.recommender.collaborative.rating;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.recommender.MatrixFactorizationRecommender;

/**
 * 
 * BiasedMF推荐器
 * 
 * <pre>
 * Biased Matrix Factorization
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class BiasedMFRecommender extends MatrixFactorizationRecommender {
	/**
	 * bias regularization
	 */
	protected float regBias;

	/**
	 * user biases
	 */
	protected DenseVector userBiases;

	/**
	 * user biases
	 */
	protected DenseVector itemBiases;

	@Override
	public void prepare(Configuration configuration, DataModule model, DataSpace space) {
		super.prepare(configuration, model, space);
		regBias = configuration.getFloat("rec.bias.regularization", 0.01F);

		// initialize the userBiased and itemBiased
		userBiases = DenseVector.valueOf(numberOfUsers);
		userBiases.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(distribution.sample().floatValue());
		});
		itemBiases = DenseVector.valueOf(numberOfItems);
		itemBiases.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(distribution.sample().floatValue());
		});
	}

	@Override
	protected void doPractice() {
		for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
			totalLoss = 0F;

			for (MatrixScalar term : trainMatrix) {
				int userIndex = term.getRow(); // user userIdx
				int itemIndex = term.getColumn(); // item itemIdx
				float rate = term.getValue(); // real rating on item
												// itemIdx rated by user
												// userIdx
				float predict = predict(userIndex, itemIndex);
				float error = rate - predict;
				totalLoss += error * error;

				// update user and item bias
				float userBias = userBiases.getValue(userIndex);
				userBiases.shiftValue(userIndex, learnRate * (error - regBias * userBias));
				totalLoss += regBias * userBias * userBias;
				float itemBias = itemBiases.getValue(itemIndex);
				itemBiases.shiftValue(itemIndex, learnRate * (error - regBias * itemBias));
				totalLoss += regBias * itemBias * itemBias;

				// update user and item factors
				for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
					float userFactor = userFactors.getValue(userIndex, factorIndex);
					float itemFactor = itemFactors.getValue(itemIndex, factorIndex);
					userFactors.shiftValue(userIndex, factorIndex, learnRate * (error * itemFactor - userRegularization * userFactor));
					itemFactors.shiftValue(itemIndex, factorIndex, learnRate * (error * userFactor - itemRegularization * itemFactor));
					totalLoss += userRegularization * userFactor * userFactor + itemRegularization * itemFactor * itemFactor;
				}
			}

			totalLoss *= 0.5D;
			if (isConverged(iterationStep) && isConverged) {
				break;
			}
			isLearned(iterationStep);
			currentLoss = totalLoss;
		}
	}

	@Override
	protected float predict(int userIndex, int itemIndex) {
		DefaultScalar scalar = DefaultScalar.getInstance();
		DenseVector userVector = userFactors.getRowVector(userIndex);
		DenseVector itemVector = itemFactors.getRowVector(itemIndex);
		float value = scalar.dotProduct(userVector, itemVector).getValue();
		value += meanOfScore + userBiases.getValue(userIndex) + itemBiases.getValue(itemIndex);
		return value;
	}

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		int userIndex = dicreteFeatures[userDimension];
		int itemIndex = dicreteFeatures[itemDimension];
		return predict(userIndex, itemIndex);
	}

}
