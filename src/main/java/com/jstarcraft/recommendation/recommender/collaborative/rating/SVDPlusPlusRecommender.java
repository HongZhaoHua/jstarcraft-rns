package com.jstarcraft.recommendation.recommender.collaborative.rating;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.recommendation.configurator.Configuration;

/**
 * 
 * SVD++推荐器
 * 
 * <pre>
 * Factorization Meets the Neighborhood: a Multifaceted Collaborative Filtering Model
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class SVDPlusPlusRecommender extends BiasedMFRecommender {
	/**
	 * item implicit feedback factors, "imp" string means implicit
	 */
	private DenseMatrix factorMatrix;

	/**
	 * implicit item regularization
	 */
	private float regImpItem;

	/*
	 * (non-Javadoc)
	 *
	 * @see net.librec.recommender.AbstractRecommender#setup()
	 */
	@Override
	public void prepare(Configuration configuration, DataModule model, DataSpace space) {
		super.prepare(configuration, model, space);
		regImpItem = configuration.getFloat("rec.impItem.regularization", 0.015F);
		factorMatrix = DenseMatrix.valueOf(numberOfItems, numberOfFactors);
		factorMatrix.iterateElement(MathCalculator.SERIAL, (element) -> {
			element.setValue(distribution.sample().floatValue());
		});
	}

	@Override
	protected void doPractice() {
		DenseVector factorVector = DenseVector.valueOf(numberOfFactors);
		for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
			totalLoss = 0F;
			for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
				SparseVector userVector = trainMatrix.getRowVector(userIndex);
				if (userVector.getElementSize() == 0) {
					continue;
				}
				for (VectorScalar outerTerm : userVector) {
					int itemIndex = outerTerm.getIndex();
					// TODO 此处可以修改为按userVector重置
					factorVector.setValues(0F);
					for (VectorScalar innerTerm : userVector) {
						factorVector.addVector(factorMatrix.getRowVector(innerTerm.getIndex()));
					}
					float scale = (float) Math.sqrt(userVector.getElementSize());
					if (scale > 0F) {
						factorVector.scaleValues(1F / scale);
					}
					float error = outerTerm.getValue() - predict(userIndex, itemIndex, factorVector);
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
						itemFactors.shiftValue(itemIndex, factorIndex, learnRate * (error * (userFactor + factorVector.getValue(factorIndex)) - itemRegularization * itemFactor));
						totalLoss += userRegularization * userFactor * userFactor + itemRegularization * itemFactor * itemFactor;
						for (VectorScalar innerTerm : userVector) {
							int index = innerTerm.getIndex();
							float factor = factorMatrix.getValue(index, factorIndex);
							factorMatrix.shiftValue(index, factorIndex, learnRate * (error * itemFactor / scale - regImpItem * factor));
							totalLoss += regImpItem * factor * factor;
						}
					}
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

	private float predict(int userIndex, int itemIndex, DenseVector factorVector) {
		float value = userBiases.getValue(userIndex) + itemBiases.getValue(itemIndex) + meanOfScore;
		// sum with user factors
		for (int index = 0; index < numberOfFactors; index++) {
			value = value + (factorVector.getValue(index) + userFactors.getValue(userIndex, index)) * itemFactors.getValue(itemIndex, index);
		}
		return value;
	}

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		int userIndex = dicreteFeatures[userDimension];
		int itemIndex = dicreteFeatures[itemDimension];
		SparseVector userVector = trainMatrix.getRowVector(userIndex);
		// TODO 此处需要重构,取消DenseVector.
		DenseVector factorVector = DenseVector.valueOf(numberOfFactors);
		// sum of implicit feedback factors of userIdx with weight Math.sqrt(1.0
		// / userItemsList.get(userIdx).size())
		for (VectorScalar term : userVector) {
			factorVector.addVector(factorMatrix.getRowVector(term.getIndex()));
		}
		float scale = (float) Math.sqrt(userVector.getElementSize());
		if (scale > 0D) {
			factorVector.scaleValues(1F / scale);
		}
		return predict(userIndex, itemIndex, factorVector);
	}

}
