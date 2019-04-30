package com.jstarcraft.recommendation.recommender.collaborative.rating;

import java.util.Map.Entry;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.DenseModule;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;
import com.jstarcraft.recommendation.recommender.ProbabilisticGraphicalRecommender;
import com.jstarcraft.recommendation.utility.GammaUtility;
import com.jstarcraft.recommendation.utility.SampleUtility;

/**
 * 
 * URP推荐器
 * 
 * <pre>
 * User Rating Profile: a LDA model for rating prediction
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class URPRecommender extends ProbabilisticGraphicalRecommender {

	private float preRMSE;

	/**
	 * number of occurrentces of entry (user, topic)
	 */
	private DenseMatrix userTopicTimes;

	/**
	 * number of occurences of users
	 */
	private DenseVector userTopicNumbers;

	/**
	 * number of occurrences of entry (topic, item)
	 */
	private DenseMatrix topicItemNumbers;

	/**
	 * P(k | u)
	 */
	private DenseMatrix userTopicProbabilities, userTopicSums;

	/**
	 * user parameters
	 */
	private DenseVector alpha;

	/**
	 * item parameters
	 */
	private DenseVector beta;

	/**
	 *
	 */
	private Table<Integer, Integer, Integer> topicAssignments;

	/**
	 * number of occurrences of entry (t, i, r)
	 */
	private int[][][] topicItemTimes; // Nkir

	/**
	 * cumulative statistics of probabilities of (t, i, r)
	 */
	private float[][][] topicItemRateSums; // PkirSum;

	/**
	 * posterior probabilities of parameters phi_{k, i, r}
	 */
	private float[][][] topicItemRateProbabilities; // Pkir;

	private DenseVector randomProbabilities;

	/** 学习矩阵与校验矩阵(TODO 将scoreMatrix划分) */
	private SparseMatrix learnMatrix, checkMatrix;

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, DenseModule model, DataSpace space) {
		super.prepare(configuration, marker, model, space);

		float checkRatio = configuration.getFloat("rec.urp.chech.ratio", 0F);
		if (checkRatio == 0F) {
			learnMatrix = trainMatrix;
			checkMatrix = null;
		} else {
			Table<Integer, Integer, Float> learnTable = HashBasedTable.create();
			Table<Integer, Integer, Float> checkTable = HashBasedTable.create();
			for (MatrixScalar term : trainMatrix) {
				int userIndex = term.getRow();
				int itemIndex = term.getColumn();
				float score = term.getValue();
				if (RandomUtility.randomFloat(1F) <= checkRatio) {
					checkTable.put(userIndex, itemIndex, score);
				} else {
					learnTable.put(userIndex, itemIndex, score);
				}
			}
			learnMatrix = SparseMatrix.valueOf(numberOfUsers, numberOfItems, learnTable);
			checkMatrix = SparseMatrix.valueOf(numberOfUsers, numberOfItems, checkTable);
		}

		// cumulative parameters
		userTopicSums = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
		topicItemRateSums = new float[numberOfFactors][numberOfItems][numberOfScores];

		// initialize count variables
		userTopicTimes = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
		userTopicNumbers = DenseVector.valueOf(numberOfUsers);

		topicItemTimes = new int[numberOfFactors][numberOfItems][numberOfScores];
		topicItemNumbers = DenseMatrix.valueOf(numberOfFactors, numberOfItems);

		float initAlpha = configuration.getFloat("rec.pgm.bucm.alpha", 1F / numberOfFactors);
		alpha = DenseVector.valueOf(numberOfFactors);
		alpha.setValues(initAlpha);

		float initBeta = configuration.getFloat("rec.pgm.bucm.beta", 1F / numberOfFactors);
		beta = DenseVector.valueOf(numberOfScores);
		beta.setValues(initBeta);

		// initialize topics
		topicAssignments = HashBasedTable.create();
		for (MatrixScalar term : learnMatrix) {
			int userIndex = term.getRow();
			int itemIndex = term.getColumn();
			float rate = term.getValue();
			int rateIndex = scoreIndexes.get(rate); // rating level 0 ~
													// numLevels
			int topicIndex = RandomUtility.randomInteger(numberOfFactors); // 0
																			// ~
			// k-1

			// Assign a topic t to pair (u, i)
			topicAssignments.put(userIndex, itemIndex, topicIndex);
			// number of pairs (u, t) in (u, i, t)
			userTopicTimes.shiftValue(userIndex, topicIndex, 1);
			// total number of items of user u
			userTopicNumbers.shiftValue(userIndex, 1);

			// number of pairs (t, i, r)
			topicItemTimes[topicIndex][itemIndex][rateIndex]++;
			// total number of words assigned to topic t
			topicItemNumbers.shiftValue(topicIndex, itemIndex, 1);
		}

		randomProbabilities = DenseVector.valueOf(numberOfFactors);
	}

	@Override
	protected void eStep() {
		float sumAlpha = alpha.getSum(false);
		float sumBeta = beta.getSum(false);

		// collapse Gibbs sampling
		for (MatrixScalar term : learnMatrix) {
			int userIndex = term.getRow();
			int itemIndex = term.getColumn();
			float rate = term.getValue();
			int rateIndex = scoreIndexes.get(rate); // rating level 0 ~
													// numLevels
			int assignmentIndex = topicAssignments.get(userIndex, itemIndex);

			userTopicTimes.shiftValue(userIndex, assignmentIndex, -1);
			userTopicNumbers.shiftValue(userIndex, -1);
			topicItemTimes[assignmentIndex][itemIndex][rateIndex]--;
			topicItemNumbers.shiftValue(assignmentIndex, itemIndex, -1);

			// 计算概率
			DefaultScalar sum = DefaultScalar.getInstance();
			sum.setValue(0F);
			randomProbabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
				int index = scalar.getIndex();
				float value = (userTopicTimes.getValue(userIndex, index) + alpha.getValue(index)) / (userTopicNumbers.getValue(userIndex) + sumAlpha) * (topicItemTimes[index][itemIndex][rateIndex] + beta.getValue(rateIndex)) / (topicItemNumbers.getValue(index, itemIndex) + sumBeta);
				sum.shiftValue(value);
				scalar.setValue(sum.getValue());
			});
			assignmentIndex = SampleUtility.binarySearch(randomProbabilities, 0, randomProbabilities.getElementSize() - 1, RandomUtility.randomFloat(sum.getValue()));

			// new topic t
			topicAssignments.put(userIndex, itemIndex, assignmentIndex);

			// add newly estimated z_i to count variables
			userTopicTimes.shiftValue(userIndex, assignmentIndex, 1);
			userTopicNumbers.shiftValue(userIndex, 1);
			topicItemTimes[assignmentIndex][itemIndex][rateIndex]++;
			topicItemNumbers.shiftValue(assignmentIndex, itemIndex, 1);
		}

	}

	/**
	 * Thomas P. Minka, Estimating a Dirichlet distribution, see Eq.(55)
	 */
	@Override
	protected void mStep() {
		float denominator;
		float value;

		// update alpha vector
		float alphaSum = alpha.getSum(false);
		float alphaDigamma = GammaUtility.digamma(alphaSum);
		float alphaValue;
		denominator = 0F;
		for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
			value = userTopicNumbers.getValue(userIndex);
			if (value != 0F) {
				denominator += GammaUtility.digamma(value + alphaSum) - alphaDigamma;
			}
		}
		for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
			alphaValue = alpha.getValue(topicIndex);
			alphaDigamma = GammaUtility.digamma(alphaValue);
			float numerator = 0F;
			for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
				value = userTopicTimes.getValue(userIndex, topicIndex);
				if (value != 0F) {
					numerator += GammaUtility.digamma(value + alphaValue) - alphaDigamma;
				}
			}
			if (numerator != 0F) {
				alpha.setValue(topicIndex, alphaValue * (numerator / denominator));
			}
		}

		// update beta_k
		float betaSum = beta.getSum(false);
		float betaDigamma = GammaUtility.digamma(betaSum);
		float betaValue;
		denominator = 0F;
		for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
			for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
				value = topicItemNumbers.getValue(topicIndex, itemIndex);
				if (value != 0F) {
					denominator += GammaUtility.digamma(value + betaSum) - betaDigamma;
				}
			}
		}
		for (int rateIndex = 0; rateIndex < numberOfScores; rateIndex++) {
			betaValue = beta.getValue(rateIndex);
			betaDigamma = GammaUtility.digamma(betaValue);
			float numerator = 0F;
			for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
				for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
					value = topicItemTimes[topicIndex][itemIndex][rateIndex];
					if (value != 0F) {
						numerator += GammaUtility.digamma(value + betaValue) - betaDigamma;
					}
				}
			}
			if (numerator != 0F) {
				beta.setValue(rateIndex, betaValue * (numerator / denominator));
			}
		}
	}

	protected void readoutParams() {
		float value = 0F;
		float sumAlpha = alpha.getSum(false);
		for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
			for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
				value = (userTopicTimes.getValue(userIndex, topicIndex) + alpha.getValue(topicIndex)) / (userTopicNumbers.getValue(userIndex) + sumAlpha);
				userTopicSums.shiftValue(userIndex, topicIndex, value);
			}
		}
		float sumBeta = beta.getSum(false);
		for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
			for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
				for (int rateIndex = 0; rateIndex < numberOfScores; rateIndex++) {
					value = (topicItemTimes[topicIndex][itemIndex][rateIndex] + beta.getValue(rateIndex)) / (topicItemNumbers.getValue(topicIndex, itemIndex) + sumBeta);
					topicItemRateSums[topicIndex][itemIndex][rateIndex] += value;
				}
			}
		}
		numberOfStatistics++;
	}

	@Override
	protected void estimateParams() {
		userTopicProbabilities = DenseMatrix.copyOf(userTopicSums);
		userTopicProbabilities.scaleValues(1F / numberOfStatistics);
		topicItemRateProbabilities = new float[numberOfFactors][numberOfItems][numberOfScores];
		for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
			for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
				for (int rateIndex = 0; rateIndex < numberOfScores; rateIndex++) {
					topicItemRateProbabilities[topicIndex][itemIndex][rateIndex] = topicItemRateSums[topicIndex][itemIndex][rateIndex] / numberOfStatistics;
				}
			}
		}
	}

	@Override
	protected boolean isConverged(int iter) {
		// TODO 此处使用validMatrix似乎更合理.
		if (checkMatrix == null) {
			return false;
		}
		// get posterior probability distribution first
		estimateParams();
		// compute current RMSE
		int count = 0;
		float sum = 0F;
		// TODO 此处使用validMatrix似乎更合理.
		for (MatrixScalar term : checkMatrix) {
			int userIndex = term.getRow();
			int itemIndex = term.getColumn();
			float rate = term.getValue();
			float predict = predict(userIndex, itemIndex);
			if (Double.isNaN(predict)) {
				continue;
			}
			float error = rate - predict;
			sum += error * error;
			count++;
		}
		float rmse = (float) Math.sqrt(sum / count);
		float delta = rmse - preRMSE;
		if (numberOfStatistics > 1 && delta > 0F) {
			return true;
		}
		preRMSE = rmse;
		return false;
	}

	private float predict(int userIndex, int itemIndex) {
		float value = 0F;
		for (Entry<Float, Integer> term : scoreIndexes.entrySet()) {
			float rate = term.getKey();
			float probability = 0F;
			for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
				probability += userTopicProbabilities.getValue(userIndex, topicIndex) * topicItemRateProbabilities[topicIndex][itemIndex][term.getValue()];
			}
			value += probability * rate;
		}
		if (value > maximumOfScore) {
			value = maximumOfScore;
		} else if (value < minimumOfScore) {
			value = minimumOfScore;
		}
		return value;
	}

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		int userIndex = dicreteFeatures[userDimension];
		int itemIndex = dicreteFeatures[itemDimension];
		return predict(userIndex, itemIndex);
	}

}
