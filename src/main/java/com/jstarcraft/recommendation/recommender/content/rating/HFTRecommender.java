package com.jstarcraft.recommendation.recommender.content.rating;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.jstarcraft.ai.data.attribute.MemoryQualityAttribute;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.model.neuralnetwork.activation.ActivationFunction;
import com.jstarcraft.ai.model.neuralnetwork.activation.SoftMaxActivationFunction;
import com.jstarcraft.ai.utility.MathUtility;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.DataSample;
import com.jstarcraft.recommendation.data.accessor.DenseModule;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;
import com.jstarcraft.recommendation.recommender.MatrixFactorizationRecommender;
import com.jstarcraft.recommendation.utility.SampleUtility;

/**
 * 
 * HFT推荐器
 * 
 * <pre>
 * Hidden factors and hidden topics: understanding rating dimensions with review text
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class HFTRecommender extends MatrixFactorizationRecommender {

	private static class Content {

		private int[] wordIndexes;

		private int[] topicIndexes;

		private Content(int[] wordIndexes) {
			this.wordIndexes = wordIndexes;
		}

		int[] getWordIndexes() {
			return wordIndexes;
		}

		int[] getTopicIndexes() {
			return topicIndexes;
		}

		void setTopicIndexes(int[] topicIndexes) {
			this.topicIndexes = topicIndexes;
		}

	}

	// TODO 考虑重构
	private Table<Integer, Integer, Content> contentMatrix;

	private DenseMatrix wordFactors;

	protected String commentField;
	protected int commentDimension;
	/** 单词数量(TODO 考虑改名为numWords) */
	private int numberOfWords;
	/**
	 * user biases
	 */
	private DenseVector userBiases;

	/**
	 * user biases
	 */
	private DenseVector itemBiases;
	/**
	 * user latent factors
	 */
	// TODO 取消,父类已实现.
	private DenseMatrix userFactors;

	/**
	 * item latent factors
	 */
	// TODO 取消,父类已实现.
	private DenseMatrix itemFactors;
	/**
	 * init mean
	 */
	// TODO 取消,父类已实现.
	private float initMean;

	/**
	 * init standard deviation
	 */
	// TODO 取消,父类已实现.
	private float initStd;
	/**
	 * bias regularization
	 */
	private float biasRegularization;
	/**
	 * user regularization
	 */
	// TODO 取消,父类已实现.
	private float userRegularization;

	/**
	 * item regularization
	 */
	// TODO 取消,父类已实现.
	private float itemRegularization;

	private DenseVector probability;

	private DenseMatrix userProbabilities;
	private DenseMatrix wordProbabilities;

	protected ActivationFunction function;

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, DenseModule model, DataSpace space) {
		super.prepare(configuration, marker, model, space);

		commentField = configuration.getString("data.model.fields.comment");
		commentDimension = model.getQualityInner(commentField);
		MemoryQualityAttribute attribute = (MemoryQualityAttribute) model.getQualityAttribute(commentDimension);
		Object[] wordValues = attribute.getDatas();

		biasRegularization = configuration.getFloat("rec.bias.regularization", 0.01F);
		userRegularization = configuration.getFloat("rec.user.regularization", 0.01F);
		itemRegularization = configuration.getFloat("rec.item.regularization", 0.01F);

		userFactors = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
		itemFactors = DenseMatrix.valueOf(numberOfItems, numberOfFactors);

		// TODO 此处需要重构initMean与initStd
		initMean = 0.0F;
		initStd = 0.1F;
		userBiases = DenseVector.valueOf(numberOfUsers);
		userBiases.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(distribution.sample().floatValue());
		});
		itemBiases = DenseVector.valueOf(numberOfItems);
		itemBiases.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(distribution.sample().floatValue());
		});

		numberOfWords = 0;
		// build review matrix and counting the number of words
		contentMatrix = HashBasedTable.create();
		Map<String, Integer> wordDictionaries = new HashMap<>();
		for (DataSample sample : marker) {
			int userIndex = sample.getDiscreteFeature(userDimension);
			int itemIndex = sample.getDiscreteFeature(itemDimension);
			int contentIndex = sample.getDiscreteFeature(commentDimension);
			String data = (String) wordValues[contentIndex];
			String[] words = data.isEmpty() ? new String[0] : data.split(":");
			for (String word : words) {
				if (!wordDictionaries.containsKey(word) && StringUtils.isNotEmpty(word)) {
					wordDictionaries.put(word, numberOfWords);
					numberOfWords++;
				}
			}
			// TODO 此处旧代码使用indexes[index] =
			// Integer.valueOf(words[index])似乎有Bug,应该使用indexes[index] =
			// wordDictionaries.get(word);
			int[] wordIndexes = new int[words.length];
			for (int index = 0; index < words.length; index++) {
				wordIndexes[index] = Integer.valueOf(words[index]);
			}
			Content content = new Content(wordIndexes);
			contentMatrix.put(userIndex, itemIndex, content);
		}

		// TODO 此处保证所有特征都会被识别
		for (Object value : wordValues) {
			String content = (String) value;
			String[] words = content.split(":");
			for (String word : words) {
				if (!wordDictionaries.containsKey(word) && StringUtils.isNotEmpty(word)) {
					wordDictionaries.put(word, numberOfWords);
					numberOfWords++;
				}
			}
		}

		logger.info("number of users : " + numberOfUsers);
		logger.info("number of Items : " + numberOfItems);
		logger.info("number of words : " + numberOfWords);

		wordFactors = DenseMatrix.valueOf(numberOfFactors, numberOfWords);
		wordFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(RandomUtility.randomFloat(0.1F));
		});

		userProbabilities = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
		wordProbabilities = DenseMatrix.valueOf(numberOfFactors, numberOfWords);
		probability = DenseVector.valueOf(numberOfFactors);
		probability.setValues(1F);

		function = new SoftMaxActivationFunction();

		for (MatrixScalar term : trainMatrix) {
			int userIndex = term.getRow(); // user
			int itemIndex = term.getColumn(); // item
			Content content = contentMatrix.get(userIndex, itemIndex);
			int[] wordIndexes = content.getWordIndexes();
			int[] topicIndexes = new int[wordIndexes.length];
			for (int wordIndex = 0; wordIndex < wordIndexes.length; wordIndex++) {
				topicIndexes[wordIndex] = RandomUtility.randomInteger(numberOfFactors);
			}
			content.setTopicIndexes(topicIndexes);
		}
		calculateThetas();
		calculatePhis();
	}

	private void sample() {
		calculateThetas();
		calculatePhis();
		for (MatrixScalar term : trainMatrix) {
			int userIndex = term.getRow(); // user
			int itemIndex = term.getColumn(); // item
			Content content = contentMatrix.get(userIndex, itemIndex);
			int[] wordIndexes = content.getWordIndexes();
			int[] topicIndexes = content.getTopicIndexes();
			sampleTopicsToWords(userIndex, wordIndexes, topicIndexes);
			// LOG.info("user:" + u + ", item:" + j + ", topics:" + s);
		}
	}

	/**
	 * Update function for thetas and phiks, check if softmax comes in to NaN and
	 * update the parameters.
	 *
	 * @param oldValues
	 *            old values of the parameter
	 * @param newValues
	 *            new values to update the parameter
	 * @return the old values if new values contain NaN
	 * @throws Exception
	 *             if error occurs
	 */
	private float[] updateArray(float[] oldValues, float[] newValues) {
		for (float value : newValues) {
			if (Float.isNaN(value)) {
				return oldValues;
			}
		}
		return newValues;
	}

	private void calculateThetas() {
		for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
			DenseVector factorVector = userFactors.getRowVector(userIndex);
			function.forward(factorVector, userProbabilities.getRowVector(userIndex));
		}
	}

	private void calculatePhis() {
		for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
			DenseVector factorVector = wordFactors.getRowVector(factorIndex);
			function.forward(factorVector, wordProbabilities.getRowVector(factorIndex));
		}
	}

	// TODO 考虑整理到Content.
	private int[] sampleTopicsToWords(int userIndex, int[] wordsIndexes, int[] topicIndexes) {
		for (int wordIndex = 0; wordIndex < wordsIndexes.length; wordIndex++) {
			int topicIndex = wordsIndexes[wordIndex];
			DefaultScalar sum = DefaultScalar.getInstance();
			sum.setValue(0F);
			probability.iterateElement(MathCalculator.SERIAL, (scalar) -> {
				int index = scalar.getIndex();
				float value = userProbabilities.getValue(userIndex, index) * wordProbabilities.getValue(index, topicIndex);
				sum.shiftValue(value);
				scalar.setValue(sum.getValue());
			});
			topicIndexes[wordIndex] = SampleUtility.binarySearch(probability, 0, probability.getElementSize() - 1, RandomUtility.randomFloat(sum.getValue()));
		}
		return topicIndexes;
	}

	/**
	 * The training approach is SGD instead of L-BFGS, so it can be slow if the
	 * dataset is big.
	 */
	@Override
	protected void doPractice() {
		for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
			// SGD training
			// TODO 此处应该修改为配置
			for (int iterationSDG = 1; iterationSDG <= 5; iterationSDG++) {
				totalLoss = 0F;
				for (MatrixScalar term : trainMatrix) {
					int userIndex = term.getRow(); // user
					int itemIndex = term.getColumn(); // item
					float rate = term.getValue();

					float predict = predict(userIndex, itemIndex);
					float error = rate - predict;
					totalLoss += error * error;

					// update factors
					float userBias = userBiases.getValue(userIndex);
					float sgd = error - biasRegularization * userBias;
					userBiases.shiftValue(userIndex, learnRate * sgd);
					// loss += regB * bu * bu;
					float itemBias = itemBiases.getValue(itemIndex);
					sgd = error - biasRegularization * itemBias;
					itemBiases.shiftValue(itemIndex, learnRate * sgd);
					// loss += regB * bj * bj;

					// TODO 此处应该重构
					Content content = contentMatrix.get(userIndex, itemIndex);
					int[] wordIndexes = content.getWordIndexes();
					if (wordIndexes.length == 0) {
						continue;
					}
					int[] topicIndexes = content.getTopicIndexes();

					for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
						float userFactor = userFactors.getValue(userIndex, factorIndex);
						float itemFactor = itemFactors.getValue(itemIndex, factorIndex);
						float userSGD = error * itemFactor - userRegularization * userFactor;
						float itemSGD = error * userFactor - itemRegularization * itemFactor;
						userFactors.shiftValue(userIndex, factorIndex, learnRate * userSGD);
						itemFactors.shiftValue(itemIndex, factorIndex, learnRate * itemSGD);
						for (int wordIndex = 0; wordIndex < wordIndexes.length; wordIndex++) {
							int topicIndex = topicIndexes[wordIndex];
							if (factorIndex == topicIndex) {
								userFactors.shiftValue(userIndex, factorIndex, learnRate * (1 - userProbabilities.getValue(userIndex, topicIndex)));
							} else {
								userFactors.shiftValue(userIndex, factorIndex, learnRate * (-userProbabilities.getValue(userIndex, topicIndex)));
							}
							totalLoss -= MathUtility.logarithm(userProbabilities.getValue(userIndex, topicIndex) * wordProbabilities.getValue(topicIndex, wordIndexes[wordIndex]), 2);
						}
					}

					for (int wordIndex = 0; wordIndex < wordIndexes.length; wordIndex++) {
						int topicIndex = topicIndexes[wordIndex];
						for (int dictionaryIndex = 0; dictionaryIndex < numberOfWords; dictionaryIndex++) {
							if (dictionaryIndex == wordIndexes[wordIndex]) {
								wordFactors.shiftValue(topicIndex, wordIndexes[wordIndex], learnRate * (-1 + wordProbabilities.getValue(topicIndex, wordIndexes[wordIndex])));
							} else {
								wordFactors.shiftValue(topicIndex, wordIndexes[wordIndex], learnRate * (wordProbabilities.getValue(topicIndex, wordIndexes[wordIndex])));
							}
						}
					}
				}
				totalLoss *= 0.5F;
			} // end of SGDtraining
			logger.info(" iter:" + iterationStep + ", loss:" + totalLoss);
			logger.info(" iter:" + iterationStep + ", sampling");
			sample();
			logger.info(" iter:" + iterationStep + ", sample finished");
		}
	}

	@Override
	protected float predict(int userIndex, int itemIndex) {
		DefaultScalar scalar = DefaultScalar.getInstance();
		DenseVector userVector = userFactors.getRowVector(userIndex);
		DenseVector itemVector = itemFactors.getRowVector(itemIndex);
		float value = scalar.dotProduct(userVector, itemVector).getValue();
		value += meanOfScore + userBiases.getValue(userIndex) + itemBiases.getValue(itemIndex);
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
