package com.jstarcraft.rns.recommend.extend.ranking;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.algorithm.similarity.Similarity;
import com.jstarcraft.ai.math.structure.matrix.SymmetryMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.common.reflection.ReflectionUtility;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.configure.Configurator;
import com.jstarcraft.rns.recommend.collaborative.ranking.RankSGDRecommender;
import com.jstarcraft.rns.recommend.exception.RecommendException;
import com.jstarcraft.rns.utility.SampleUtility;

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
	 * @throws RecommendException
	 *             if error occurs
	 */
	@Override
	public void prepare(Configurator configuration, DataModule model, DataSpace space) {
		super.prepare(configuration, model, space);
		similarityFilter = configuration.getFloat("recommender.sim.filter", 4F);
		float denominator = 0F;
		itemWeights = DenseVector.valueOf(itemSize);
		for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
			float numerator = scoreMatrix.getColumnScope(itemIndex);
			denominator = denominator < numerator ? numerator : denominator;
			itemWeights.setValue(itemIndex, numerator);
		}
		// compute item relative importance
		for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
			itemWeights.setValue(itemIndex, itemWeights.getValue(itemIndex) / denominator);
		}

		// compute item correlations by cosine similarity
		// TODO 修改为配置枚举
		try {
			Class<Similarity> similarityClass = (Class<Similarity>) Class.forName(configuration.getString("recommender.similarity.class"));
			Similarity similarity = ReflectionUtility.getInstance(similarityClass);
			itemCorrelations = similarity.makeSimilarityMatrix(scoreMatrix, true, configuration.getFloat("recommender.similarity.shrinkage", 0F));
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		}

		userIndexes = new LinkedList<>();
		for (int userIndex = 0; userIndex < userSize; userIndex++) {
			if (scoreMatrix.getRowVector(userIndex).getElementSize() > 0) {
				userIndexes.add(userIndex);
			}
		}
		userIndexes = new ArrayList<>(userIndexes);
	}

	/**
	 * train model
	 *
	 * @throws RecommendException
	 *             if error occurs
	 */
	@Override
	protected void doPractice() {
		List<IntSet> userItemSet = getUserItemSet(scoreMatrix);
		for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
			totalError = 0F;
			// for each rated user-item (u,i) pair
			for (int userIndex : userIndexes) {
				SparseVector userVector = scoreMatrix.getRowVector(userIndex);
				IntSet itemSet = userItemSet.get(userIndex);
				for (VectorScalar term : userVector) {
					// each rated item i
					int positiveItemIndex = term.getIndex();
					float positiveScore = term.getValue();
					int negativeItemIndex = -1;
					do {
						// draw an item j with probability proportional to
						// popularity
						negativeItemIndex = SampleUtility.binarySearch(itemProbabilities, 0, itemProbabilities.getElementSize() - 1, RandomUtility.randomFloat(itemProbabilities.getValue(itemProbabilities.getElementSize() - 1)));
						// ensure that it is unrated by user u
					} while (itemSet.contains(negativeItemIndex));
					float negativeScore = 0F;
					// compute predictions
					float positivePredict = predict(userIndex, positiveItemIndex), negativePredict = predict(userIndex, negativeItemIndex);
					float distance = (float) Math.sqrt(1 - Math.tanh(itemCorrelations.getValue(positiveItemIndex, negativeItemIndex) * similarityFilter));
					float itemWeight = itemWeights.getValue(negativeItemIndex);
					float error = itemWeight * (positivePredict - negativePredict - distance * (positiveScore - negativeScore));
					totalError += error * error;

					// update vectors
					float learnFactor = learnRatio * error;
					for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
						float userFactor = userFactors.getValue(userIndex, factorIndex);
						float positiveItemFactor = itemFactors.getValue(positiveItemIndex, factorIndex);
						float negativeItemFactor = itemFactors.getValue(negativeItemIndex, factorIndex);
						userFactors.shiftValue(userIndex, factorIndex, -learnFactor * (positiveItemFactor - negativeItemFactor));
						itemFactors.shiftValue(positiveItemIndex, factorIndex, -learnFactor * userFactor);
						itemFactors.shiftValue(negativeItemIndex, factorIndex, learnFactor * userFactor);
					}
				}
			}

			totalError *= 0.5F;
			if (isConverged(epocheIndex) && isConverged) {
				break;
			}
			isLearned(epocheIndex);
			currentError = totalError;
		}
	}

}
