package com.jstarcraft.recommendation.recommender.collaborative.ranking;

import java.util.HashSet;
import java.util.Set;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.recommendation.configurator.Configuration;
import com.jstarcraft.recommendation.recommender.MatrixFactorizationRecommender;
import com.jstarcraft.recommendation.utility.LogisticUtility;

/**
 * 
 * Random Guess推荐器
 * 
 * <pre>
 * GBPR: Group Preference Based Bayesian Personalized Ranking for One-Class Collaborative Filtering
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class GBPRRecommender extends MatrixFactorizationRecommender {

	private float rho;

	private int gLen;

	/**
	 * bias regularization
	 */
	private float regBias;

	/**
	 * items biases vector
	 */
	private DenseVector itemBiases;

	@Override
	public void prepare(Configuration configuration, DataModule model, DataSpace space) {
		super.prepare(configuration, model, space);
		itemBiases = DenseVector.valueOf(numberOfItems);
		itemBiases.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(RandomUtility.randomFloat(1F));
		});

		rho = configuration.getFloat("rec.gpbr.rho", 1.5f);
		gLen = configuration.getInteger("rec.gpbr.gsize", 2);
	}

	@Override
	protected void doPractice() {
		for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
			totalLoss = 0F;
			// TODO 考虑重构
			DenseMatrix userDeltas = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
			DenseMatrix itemDeltas = DenseMatrix.valueOf(numberOfItems, numberOfFactors);

			for (int sampleIndex = 0, sampleTimes = numberOfUsers * 100; sampleIndex < sampleTimes; sampleIndex++) {
				int userIndex, positiveItemIndex, negativeItemIndex;
				SparseVector userVector;
				do {
					userIndex = RandomUtility.randomInteger(numberOfUsers);
					userVector = trainMatrix.getRowVector(userIndex);
				} while (userVector.getElementSize() == 0);
				positiveItemIndex = userVector.getIndex(RandomUtility.randomInteger(userVector.getElementSize()));

				// users group Set
				Set<Integer> memberSet = new HashSet<>();
				SparseVector positiveItemVector = trainMatrix.getColumnVector(positiveItemIndex);
				if (positiveItemVector.getElementSize() <= gLen) {
					for (VectorScalar entry : positiveItemVector) {
						memberSet.add(entry.getIndex());
					}
				} else {
					memberSet.add(userIndex); // u in G
					while (memberSet.size() < gLen) {
						memberSet.add(positiveItemVector.getIndex(RandomUtility.randomInteger(positiveItemVector.getElementSize())));
					}
				}
				float positiveRate = predict(userIndex, positiveItemIndex, memberSet);
				negativeItemIndex = RandomUtility.randomInteger(numberOfItems - userVector.getElementSize());
				for (VectorScalar term : userVector) {
					if (negativeItemIndex >= term.getIndex()) {
						negativeItemIndex++;
					} else {
						break;
					}
				}
				float negativeRate = predict(userIndex, negativeItemIndex);
				float error = positiveRate - negativeRate;
				float value = (float) -Math.log(LogisticUtility.getValue(error));
				totalLoss += value;
				value = LogisticUtility.getValue(-error);

				// update bi, bj
				float positiveBias = itemBiases.getValue(positiveItemIndex);
				itemBiases.shiftValue(positiveItemIndex, learnRate * (value - regBias * positiveBias));
				float negativeBias = itemBiases.getValue(negativeItemIndex);
				itemBiases.shiftValue(negativeItemIndex, learnRate * (-value - regBias * negativeBias));

				// update Pw
				float averageWeight = 1F / memberSet.size();
				float memberSums[] = new float[numberOfFactors];
				for (int memberIndex : memberSet) {
					float delta = memberIndex == userIndex ? 1F : 0F;
					for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
						float memberFactor = userFactors.getValue(memberIndex, factorIndex);
						float positiveFactor = itemFactors.getValue(positiveItemIndex, factorIndex);
						float negativeFactor = itemFactors.getValue(negativeItemIndex, factorIndex);
						float deltaGroup = rho * averageWeight * positiveFactor + (1 - rho) * delta * positiveFactor - delta * negativeFactor;
						userDeltas.shiftValue(memberIndex, factorIndex, learnRate * (value * deltaGroup - userRegularization * memberFactor));
						memberSums[factorIndex] += memberFactor;
					}
				}

				// update itemFactors
				for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
					float userFactor = userFactors.getValue(userIndex, factorIndex);
					float positiveFactor = itemFactors.getValue(positiveItemIndex, factorIndex);
					float negativeFactor = itemFactors.getValue(negativeItemIndex, factorIndex);
					float positiveDelta = rho * averageWeight * memberSums[factorIndex] + (1 - rho) * userFactor;
					itemDeltas.shiftValue(positiveItemIndex, factorIndex, learnRate * (value * positiveDelta - itemRegularization * positiveFactor));
					float negativeDelta = -userFactor;
					itemDeltas.shiftValue(negativeItemIndex, factorIndex, learnRate * (value * negativeDelta - itemRegularization * negativeFactor));
				}
			}
			userFactors.addMatrix(userDeltas, false);
			itemFactors.addMatrix(itemDeltas, false);

			if (isConverged(iterationStep) && isConverged) {
				break;
			}
			isLearned(iterationStep);
			currentLoss = totalLoss;
		}
	}

	private float predict(int userIndex, int itemIndex, Set<Integer> memberIndexes) {
		DefaultScalar scalar = DefaultScalar.getInstance();
		DenseVector userVector = userFactors.getRowVector(userIndex);
		DenseVector itemVector = itemFactors.getRowVector(itemIndex);
		float value = itemBiases.getValue(itemIndex) + scalar.dotProduct(userVector, itemVector).getValue();
		float sum = 0F;
		for (int memberIndex : memberIndexes) {
			userVector = userFactors.getRowVector(memberIndex);
			sum += scalar.dotProduct(userVector, itemVector).getValue();
		}
		float groupRate = sum / memberIndexes.size() + itemBiases.getValue(itemIndex);
		return rho * groupRate + (1 - rho) * value;
	}

	@Override
	protected float predict(int userIndex, int itemIndex) {
		DefaultScalar scalar = DefaultScalar.getInstance();
		DenseVector userVector = userFactors.getRowVector(userIndex);
		DenseVector itemVector = itemFactors.getRowVector(itemIndex);
		return itemBiases.getValue(itemIndex) + scalar.dotProduct(userVector, itemVector).getValue();
	}

	@Override
	public float predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
		return predict(userIndex, itemIndex);
	}

}
