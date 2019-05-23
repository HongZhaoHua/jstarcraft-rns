package com.jstarcraft.rns.recommender.collaborative;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.configurator.Configuration;
import com.jstarcraft.rns.recommender.ProbabilisticGraphicalRecommender;
import com.jstarcraft.rns.utility.SampleUtility;

/**
 * 
 * BH Free推荐器
 * 
 * <pre>
 * Balancing Prediction and Recommendation Accuracy: Hierarchical Latent Factors for Preference Data
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public abstract class BHFreeRecommender extends ProbabilisticGraphicalRecommender {

	private static class TopicTerm {

		private int userTopic;

		private int itemTopic;

		private int rateIndex;

		private TopicTerm(int userTopic, int itemTopic, int rateIndex) {
			this.userTopic = userTopic;
			this.itemTopic = itemTopic;
			this.rateIndex = rateIndex;
		}

		void update(int userTopic, int itemTopic) {
			this.userTopic = userTopic;
			this.itemTopic = itemTopic;
		}

		public int getUserTopic() {
			return userTopic;
		}

		public int getItemTopic() {
			return itemTopic;
		}

		public int getRateIndex() {
			return rateIndex;
		}

	}

	private Table<Integer, Integer, TopicTerm> topicMatrix;

	private float initGamma, initSigma, initAlpha, initBeta;

	/**
	 * number of user communities
	 */
	protected int numberOfUserTopics; // K

	/**
	 * number of item categories
	 */
	protected int numberOfItemTopics; // L

	/**
	 * evaluation of the user u which have been assigned to the user topic k
	 */
	private DenseMatrix user2TopicNumbers;

	/**
	 * observations for the user
	 */
	private DenseVector userNumbers;

	/**
	 * observations associated with community k
	 */
	private DenseVector userTopicNumbers;

	/**
	 * number of user communities * number of topics
	 */
	private DenseMatrix userTopic2ItemTopicNumbers; // Nkl

	/**
	 * number of user communities * number of topics * number of ratings
	 */
	private int[][][] userTopic2ItemTopicRateNumbers, userTopic2ItemTopicItemNumbers; // Nklr,
	// Nkli;

	// parameters
	protected DenseMatrix user2TopicProbabilities, userTopic2ItemTopicProbabilities;
	protected DenseMatrix user2TopicSums, userTopic2ItemTopicSums;
	protected double[][][] userTopic2ItemTopicRateProbabilities, userTopic2ItemTopicItemProbabilities;
	protected double[][][] userTopic2ItemTopicRateSums, userTopic2ItemTopicItemSums;

	private DenseMatrix topicProbabilities;
	private DenseVector userProbabilities;
	private DenseVector itemProbabilities;

	@Override
	public void prepare(Configuration configuration, DataModule model, DataSpace space) {
		super.prepare(configuration, model, space);
		numberOfUserTopics = configuration.getInteger("rec.bhfree.user.topic.number", 10);
		numberOfItemTopics = configuration.getInteger("rec.bhfree.item.topic.number", 10);
		initAlpha = configuration.getFloat("rec.bhfree.alpha", 1.0f / numberOfUserTopics);
		initBeta = configuration.getFloat("rec.bhfree.beta", 1.0f / numberOfItemTopics);
		initGamma = configuration.getFloat("rec.bhfree.gamma", 1.0f / numberOfScores);
		initSigma = configuration.getFloat("rec.sigma", 1.0f / numberOfItems);
		numberOfScores = scoreIndexes.size();

		// TODO 考虑重构(整合为UserTopic对象)
		user2TopicNumbers = DenseMatrix.valueOf(numberOfUsers, numberOfUserTopics);
		userNumbers = DenseVector.valueOf(numberOfUsers);

		userTopic2ItemTopicNumbers = DenseMatrix.valueOf(numberOfUserTopics, numberOfItemTopics);
		userTopicNumbers = DenseVector.valueOf(numberOfUserTopics);

		userTopic2ItemTopicRateNumbers = new int[numberOfUserTopics][numberOfItemTopics][numberOfScores];
		userTopic2ItemTopicItemNumbers = new int[numberOfUserTopics][numberOfItemTopics][numberOfItems];

		topicMatrix = HashBasedTable.create();

		for (MatrixScalar term : scoreMatrix) {
			int userIndex = term.getRow();
			int itemIndex = term.getColumn();
			float rate = term.getValue();
			int rateIndex = scoreIndexes.get(rate);
			int userTopic = RandomUtility.randomInteger(numberOfUserTopics); // user's
			// topic
			// k
			int itemTopic = RandomUtility.randomInteger(numberOfItemTopics); // item's
			// topic
			// l

			user2TopicNumbers.shiftValue(userIndex, userTopic, 1F);
			userNumbers.shiftValue(userIndex, 1F);
			userTopic2ItemTopicNumbers.shiftValue(userTopic, itemTopic, 1F);
			userTopicNumbers.shiftValue(userTopic, 1F);
			userTopic2ItemTopicRateNumbers[userTopic][itemTopic][rateIndex]++;
			userTopic2ItemTopicItemNumbers[userTopic][itemTopic][itemIndex]++;
			TopicTerm topic = new TopicTerm(userTopic, itemTopic, rateIndex);
			topicMatrix.put(userIndex, itemIndex, topic);
		}

		// parameters
		// TODO 考虑重构为一个对象
		user2TopicSums = DenseMatrix.valueOf(numberOfUsers, numberOfUserTopics);
		userTopic2ItemTopicSums = DenseMatrix.valueOf(numberOfUserTopics, numberOfItemTopics);
		userTopic2ItemTopicRateSums = new double[numberOfUserTopics][numberOfItemTopics][numberOfScores];
		userTopic2ItemTopicRateProbabilities = new double[numberOfUserTopics][numberOfItemTopics][numberOfScores];
		userTopic2ItemTopicItemSums = new double[numberOfUserTopics][numberOfItemTopics][numberOfItems];
		userTopic2ItemTopicItemProbabilities = new double[numberOfUserTopics][numberOfItemTopics][numberOfItems];

		topicProbabilities = DenseMatrix.valueOf(numberOfUserTopics, numberOfItemTopics);
		userProbabilities = DenseVector.valueOf(numberOfUserTopics);
		itemProbabilities = DenseVector.valueOf(numberOfItemTopics);
	}

	@Override
	protected void eStep() {
		for (Cell<Integer, Integer, TopicTerm> term : topicMatrix.cellSet()) {
			int userIndex = term.getRowKey();
			int itemIndex = term.getColumnKey();
			TopicTerm topicTerm = term.getValue();
			int rateIndex = topicTerm.getRateIndex();
			int userTopic = topicTerm.getUserTopic();
			int itemTopic = topicTerm.getItemTopic();

			user2TopicNumbers.shiftValue(userIndex, userTopic, -1F);
			userNumbers.shiftValue(userIndex, -1F);
			userTopic2ItemTopicNumbers.shiftValue(userTopic, itemTopic, -1F);
			userTopicNumbers.shiftValue(userTopic, -1F);
			userTopic2ItemTopicRateNumbers[userTopic][itemTopic][rateIndex]--;
			userTopic2ItemTopicItemNumbers[userTopic][itemTopic][itemIndex]--;

			// normalization
			int userTopicIndex = userTopic;
			int itemTopicIndex = itemTopic;
			topicProbabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
				float value = (user2TopicNumbers.getValue(userIndex, userTopicIndex) + initAlpha) / (userNumbers.getValue(userIndex) + numberOfUserTopics * initAlpha);
				value *= (userTopic2ItemTopicNumbers.getValue(userTopicIndex, itemTopicIndex) + initBeta) / (userTopicNumbers.getValue(userTopicIndex) + numberOfItemTopics * initBeta);
				value *= (userTopic2ItemTopicRateNumbers[userTopicIndex][itemTopicIndex][rateIndex] + initGamma) / (userTopic2ItemTopicNumbers.getValue(userTopicIndex, itemTopicIndex) + numberOfScores * initGamma);
				value *= (userTopic2ItemTopicItemNumbers[userTopicIndex][itemTopicIndex][itemIndex] + initSigma) / (userTopic2ItemTopicNumbers.getValue(userTopicIndex, itemTopicIndex) + numberOfItems * initSigma);
				scalar.setValue(value);
			});

			// 计算概率
			DefaultScalar sum = DefaultScalar.getInstance();
			sum.setValue(0F);
			userProbabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
				int index = scalar.getIndex();
				float value = topicProbabilities.getRowVector(index).getSum(false);
				sum.shiftValue(value);
				scalar.setValue(sum.getValue());
			});
			userTopic = SampleUtility.binarySearch(userProbabilities, 0, userProbabilities.getElementSize() - 1, RandomUtility.randomFloat(sum.getValue()));
			sum.setValue(0F);
			itemProbabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
				int index = scalar.getIndex();
				float value = topicProbabilities.getColumnVector(index).getSum(false);
				sum.shiftValue(value);
				scalar.setValue(sum.getValue());
			});
			itemTopic = SampleUtility.binarySearch(itemProbabilities, 0, itemProbabilities.getElementSize() - 1, RandomUtility.randomFloat(sum.getValue()));

			topicTerm.update(userTopic, itemTopic);
			// add statistic
			user2TopicNumbers.shiftValue(userIndex, userTopic, 1F);
			userNumbers.shiftValue(userIndex, 1F);
			userTopic2ItemTopicNumbers.shiftValue(userTopic, itemTopic, 1F);
			userTopicNumbers.shiftValue(userTopic, 1F);
			userTopic2ItemTopicRateNumbers[userTopic][itemTopic][rateIndex]++;
			userTopic2ItemTopicItemNumbers[userTopic][itemTopic][itemIndex]++;
		}

	}

	@Override
	protected void mStep() {

	}

	@Override
	protected void readoutParams() {
		for (int userTopic = 0; userTopic < numberOfUserTopics; userTopic++) {
			for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
				user2TopicSums.shiftValue(userIndex, userTopic, (user2TopicNumbers.getValue(userIndex, userTopic) + initAlpha) / (userNumbers.getValue(userIndex) + numberOfUserTopics * initAlpha));
			}
			for (int itemTopic = 0; itemTopic < numberOfItemTopics; itemTopic++) {
				userTopic2ItemTopicSums.shiftValue(userTopic, itemTopic, (userTopic2ItemTopicNumbers.getValue(userTopic, itemTopic) + initBeta) / (userTopicNumbers.getValue(userTopic) + numberOfItemTopics * initBeta));
				for (int rateIndex = 0; rateIndex < numberOfScores; rateIndex++) {
					userTopic2ItemTopicRateSums[userTopic][itemTopic][rateIndex] += (userTopic2ItemTopicRateNumbers[userTopic][itemTopic][rateIndex] + initGamma) / (userTopic2ItemTopicNumbers.getValue(userTopic, itemTopic) + numberOfScores * initGamma);
				}
				for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
					userTopic2ItemTopicItemSums[userTopic][itemTopic][itemIndex] += (userTopic2ItemTopicItemNumbers[userTopic][itemTopic][itemIndex] + initSigma) / (userTopic2ItemTopicNumbers.getValue(userTopic, itemTopic) + numberOfItems * initSigma);
				}
			}
		}
		numberOfStatistics++;
	}

	@Override
	protected void estimateParams() {
		float scale = 1F / numberOfStatistics;
		user2TopicProbabilities = DenseMatrix.copyOf(user2TopicSums);
		user2TopicProbabilities.scaleValues(scale);
		userTopic2ItemTopicProbabilities = DenseMatrix.copyOf(userTopic2ItemTopicSums);
		userTopic2ItemTopicProbabilities.scaleValues(scale);
		for (int userTopic = 0; userTopic < numberOfUserTopics; userTopic++) {
			for (int itemTopic = 0; itemTopic < numberOfItemTopics; itemTopic++) {
				for (int rateIndex = 0; rateIndex < numberOfScores; rateIndex++) {
					userTopic2ItemTopicRateProbabilities[userTopic][itemTopic][rateIndex] = userTopic2ItemTopicRateSums[userTopic][itemTopic][rateIndex] * scale;
				}
				for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
					userTopic2ItemTopicItemProbabilities[userTopic][itemTopic][itemIndex] = userTopic2ItemTopicItemSums[userTopic][itemTopic][itemIndex] * scale;
				}
			}
		}
	}

}
