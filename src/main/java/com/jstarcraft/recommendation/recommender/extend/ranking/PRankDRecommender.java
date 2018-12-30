package com.jstarcraft.recommendation.recommender.extend.ranking;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.jstarcraft.ai.math.algorithm.similarity.Similarity;
import com.jstarcraft.ai.math.structure.matrix.SymmetryMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.core.utility.ReflectionUtility;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.InstanceAccessor;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;
import com.jstarcraft.recommendation.exception.RecommendationException;
import com.jstarcraft.recommendation.recommender.collaborative.ranking.RankSGDRecommender;
import com.jstarcraft.recommendation.utility.SampleUtility;

import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * 
 * PRankD推荐器
 * 
 * <pre>
 * Personalised ranking with diversity
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class PRankDRecommender extends RankSGDRecommender {
	/**
	 * item importance
	 */
	private DenseVector itemWeights;

	/**
	 * item correlations
	 */
	private SymmetryMatrix itemCorrelations;

	/**
	 * similarity filter
	 */
	private float similarityFilter;

	// TODO 考虑重构到父类
	private List<Integer> userIndexes;

	/**
	 * initialization
	 *
	 * @throws RecommendationException
	 *             if error occurs
	 */
	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, InstanceAccessor model, DataSpace space) {
		super.prepare(configuration, marker, model, space);
		similarityFilter = configuration.getFloat("rec.sim.filter", 4F);
		float denominator = 0F;
		itemWeights = DenseVector.valueOf(numberOfItems);
		for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
			float numerator = trainMatrix.getColumnScope(itemIndex);
			denominator = denominator < numerator ? numerator : denominator;
			itemWeights.setValue(itemIndex, numerator);
		}
		// compute item relative importance
		for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
			itemWeights.setValue(itemIndex, itemWeights.getValue(itemIndex) / denominator);
		}

		// compute item correlations by cosine similarity
		// TODO 修改为配置枚举
		try {
			Class<Similarity> similarityClass = (Class<Similarity>) Class.forName(configuration.getString("rec.similarity.class"));
			Similarity similarity = ReflectionUtility.getInstance(similarityClass);
			itemCorrelations = similarity.makeSimilarityMatrix(trainMatrix, true, configuration.getFloat("rec.similarity.shrinkage", 0F));
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		}

		userIndexes = new LinkedList<>();
		for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
			if (trainMatrix.getRowVector(userIndex).getElementSize() > 0) {
				userIndexes.add(userIndex);
			}
		}
		userIndexes = new ArrayList<>(userIndexes);
	}

	/**
	 * train model
	 *
	 * @throws RecommendationException
	 *             if error occurs
	 */
	@Override
	protected void doPractice() {
		List<IntSet> userItemSet = getUserItemSet(trainMatrix);
		for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
			totalLoss = 0F;
			// for each rated user-item (u,i) pair
			for (int userIndex : userIndexes) {
				SparseVector userVector = trainMatrix.getRowVector(userIndex);
				IntSet itemSet = userItemSet.get(userIndex);
				for (VectorScalar term : userVector) {
					// each rated item i
					int positiveItemIndex = term.getIndex();
					float positiveRate = term.getValue();
					int negativeItemIndex = -1;
					do {
						// draw an item j with probability proportional to
						// popularity
						negativeItemIndex = SampleUtility.binarySearch(itemProbabilities, 0, itemProbabilities.getElementSize() - 1, RandomUtility.randomFloat(itemProbabilities.getValue(itemProbabilities.getElementSize() - 1)));
						// ensure that it is unrated by user u
					} while (itemSet.contains(negativeItemIndex));
					float negativeRate = 0f;
					// compute predictions
					float positivePredict = predict(userIndex, positiveItemIndex), negativePredict = predict(userIndex, negativeItemIndex);
					float distance = (float) Math.sqrt(1 - Math.tanh(itemCorrelations.getValue(positiveItemIndex, negativeItemIndex) * similarityFilter));
					float itemWeight = itemWeights.getValue(negativeItemIndex);
					float error = itemWeight * (positivePredict - negativePredict - distance * (positiveRate - negativeRate));
					totalLoss += error * error;

					// update vectors
					float learnFactor = learnRate * error;
					for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
						float userFactor = userFactors.getValue(userIndex, factorIndex);
						float positiveItemFactor = itemFactors.getValue(positiveItemIndex, factorIndex);
						float negativeItemFactor = itemFactors.getValue(negativeItemIndex, factorIndex);
						userFactors.shiftValue(userIndex, factorIndex, -learnFactor * (positiveItemFactor - negativeItemFactor));
						itemFactors.shiftValue(positiveItemIndex, factorIndex, -learnFactor * userFactor);
						itemFactors.shiftValue(negativeItemIndex, factorIndex, learnFactor * userFactor);
					}
				}
			}

			totalLoss *= 0.5F;
			if (isConverged(iterationStep) && isConverged) {
				break;
			}
			isLearned(iterationStep);
			currentLoss = totalLoss;
		}
	}

}
