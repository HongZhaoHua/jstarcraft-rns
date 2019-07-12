package com.jstarcraft.rns.recommend.collaborative.ranking;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.configurator.Configuration;
import com.jstarcraft.rns.recommend.MatrixFactorizationRecommender;
import com.jstarcraft.rns.utility.LogisticUtility;

/**
 * 
 * WARP推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class WARPMFRecommender extends MatrixFactorizationRecommender {

	private int lossType;

	private float epsilon;

	private float[] orderLosses;

	@Override
	public void prepare(Configuration configuration, DataModule model, DataSpace space) {
		super.prepare(configuration, model, space);

		lossType = configuration.getInteger("losstype", 3);
		epsilon = configuration.getFloat("epsilon");
		orderLosses = new float[numberOfItems - 1];
		float orderLoss = 0F;
		for (int orderIndex = 1; orderIndex < numberOfItems; orderIndex++) {
			orderLoss += 1D / orderIndex;
			orderLosses[orderIndex - 1] = orderLoss;
		}
		for (int rankIndex = 1; rankIndex < numberOfItems; rankIndex++) {
			orderLosses[rankIndex - 1] /= orderLoss;
		}
	}

	@Override
	protected void doPractice() {
		int Y, N;

		for (int epochIndex = 1; epochIndex <= numberOfEpoches; epochIndex++) {
			totalLoss = 0F;
			for (int sampleIndex = 0, sampleTimes = numberOfUsers * 100; sampleIndex < sampleTimes; sampleIndex++) {
				int userIndex, positiveItemIndex, negativeItemIndex;
				float positiveScore;
				float negativeScore;
				while (true) {
					userIndex = RandomUtility.randomInteger(numberOfUsers);
					SparseVector userVector = scoreMatrix.getRowVector(userIndex);
					if (userVector.getElementSize() == 0 || userVector.getElementSize() == numberOfItems) {
						continue;
					}

					N = 0;
					Y = numberOfItems - scoreMatrix.getRowScope(userIndex);
					positiveItemIndex = userVector.getIndex(RandomUtility.randomInteger(userVector.getElementSize()));
					positiveScore = predict(userIndex, positiveItemIndex);
					do {
						N++;
						negativeItemIndex = RandomUtility.randomInteger(numberOfItems - userVector.getElementSize());
						for (int index = 0, size = userVector.getElementSize(); index < size; index++) {
							if (negativeItemIndex >= userVector.getIndex(index)) {
								negativeItemIndex++;
								continue;
							}
							break;
						}
						negativeScore = predict(userIndex, negativeItemIndex);
					} while ((positiveScore - negativeScore > epsilon) && N < Y - 1);
					break;
				}
				// update parameters
				float error = positiveScore - negativeScore;

				float gradient = calaculateGradientValue(lossType, error);
				int orderIndex = (int) ((Y - 1) / N);
				float orderLoss = orderLosses[orderIndex];
				gradient = gradient * orderLoss;

				totalLoss += -Math.log(LogisticUtility.getValue(error));

				for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
					float userFactor = userFactors.getValue(userIndex, factorIndex);
					float positiveFactor = itemFactors.getValue(positiveItemIndex, factorIndex);
					float negativeFactor = itemFactors.getValue(negativeItemIndex, factorIndex);

					userFactors.shiftValue(userIndex, factorIndex, learnRate * (gradient * (positiveFactor - negativeFactor) - userRegularization * userFactor));
					itemFactors.shiftValue(positiveItemIndex, factorIndex, learnRate * (gradient * userFactor - itemRegularization * positiveFactor));
					itemFactors.shiftValue(negativeItemIndex, factorIndex, learnRate * (gradient * (-userFactor) - itemRegularization * negativeFactor));
					totalLoss += userRegularization * userFactor * userFactor + itemRegularization * positiveFactor * positiveFactor + itemRegularization * negativeFactor * negativeFactor;
				}
			}

			if (isConverged(epochIndex) && isConverged) {
				break;
			}
			isLearned(epochIndex);
			currentLoss = totalLoss;
		}
	}

}
