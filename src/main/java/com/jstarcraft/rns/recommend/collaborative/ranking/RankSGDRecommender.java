package com.jstarcraft.rns.recommend.collaborative.ranking;

import java.util.List;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.configure.Configuration;
import com.jstarcraft.rns.recommend.MatrixFactorizationRecommender;
import com.jstarcraft.rns.utility.SampleUtility;

import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * 
 * Rank SGD推荐器
 * 
 * <pre>
 * Collaborative Filtering Ensemble for Ranking
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class RankSGDRecommender extends MatrixFactorizationRecommender {
	// item sampling probabilities sorted ascendingly

	protected DenseVector itemProbabilities;

	@Override
	public void prepare(Configuration configuration, DataModule model, DataSpace space) {
		super.prepare(configuration, model, space);
		// compute item sampling probability
		DefaultScalar sum = DefaultScalar.getInstance();
		sum.setValue(0F);
		itemProbabilities = DenseVector.valueOf(numberOfItems);
		itemProbabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			int index = scalar.getIndex();
			float userSize = scoreMatrix.getColumnScope(index);
			// sample items based on popularity
			float value = (userSize + 0F) / numberOfActions;
			sum.shiftValue(value);
			scalar.setValue(sum.getValue());
		});
	}

	@Override
	protected void doPractice() {
		List<IntSet> userItemSet = getUserItemSet(scoreMatrix);
		for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
			totalLoss = 0F;
			// for each rated user-item (u,i) pair
			for (MatrixScalar term : scoreMatrix) {
				int userIndex = term.getRow();
				IntSet itemSet = userItemSet.get(userIndex);
				int positiveItemIndex = term.getColumn();
				float positiveRate = term.getValue();
				int negativeItemIndex = -1;

				do {
					// draw an item j with probability proportional to
					// popularity
					negativeItemIndex = SampleUtility.binarySearch(itemProbabilities, 0, itemProbabilities.getElementSize() - 1, RandomUtility.randomFloat(itemProbabilities.getValue(itemProbabilities.getElementSize() - 1)));
					// ensure that it is unrated by user u
				} while (itemSet.contains(negativeItemIndex));

				float negativeRate = 0F;
				// compute predictions
				float error = (predict(userIndex, positiveItemIndex) - predict(userIndex, negativeItemIndex)) - (positiveRate - negativeRate);
				totalLoss += error * error;

				// update vectors
				float value = learnRate * error;
				for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
					float userFactor = userFactors.getValue(userIndex, factorIndex);
					float positiveItemFactor = itemFactors.getValue(positiveItemIndex, factorIndex);
					float negativeItemFactor = itemFactors.getValue(negativeItemIndex, factorIndex);

					userFactors.shiftValue(userIndex, factorIndex, -value * (positiveItemFactor - negativeItemFactor));
					itemFactors.shiftValue(positiveItemIndex, factorIndex, -value * userFactor);
					itemFactors.shiftValue(negativeItemIndex, factorIndex, value * userFactor);
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

}
