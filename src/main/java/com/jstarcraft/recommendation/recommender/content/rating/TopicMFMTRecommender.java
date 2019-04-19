package com.jstarcraft.recommendation.recommender.content.rating;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.ai.model.neuralnetwork.activation.ActivationFunction;
import com.jstarcraft.ai.model.neuralnetwork.activation.SoftMaxActivationFunction;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.DataSample;
import com.jstarcraft.recommendation.data.accessor.InstanceAccessor;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;
import com.jstarcraft.recommendation.recommender.MatrixFactorizationRecommender;

/**
 * 
 * TopicMF MT推荐器
 * 
 * <pre>
 * TopicMF: Simultaneously Exploiting Ratings and Reviews for Recommendation
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class TopicMFMTRecommender extends MatrixFactorizationRecommender {

	protected String commentField;
	protected int commentDimension;
	protected SparseMatrix W;
	protected DenseMatrix documentFactors;
	protected DenseMatrix wordFactors;
	protected float K;
	protected DenseVector userBiases;
	protected DenseVector itemBiases;
	// TODO 准备取消,父类已实现.
	protected DenseMatrix userFactors;
	protected DenseMatrix itemFactors;
	// TODO topic似乎就是factor?
	protected int numberOfTopics;
	protected int numberOfWords;
	protected int numberOfDocuments;

	protected float lambda, lambdaU, lambdaV, lambdaB;

	protected Table<Integer, Integer, Integer> userItemToDocument;
	// TODO 准备取消,父类已实现.
	protected float initMean;
	protected float initStd;

	protected DenseVector topicVector;
	protected ActivationFunction function;

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, InstanceAccessor model, DataSpace space) {
		super.prepare(configuration, marker, model, space);

		commentField = configuration.getString("data.model.fields.comment");
		commentDimension = model.getDiscreteDimension(commentField);
		Object[] documentValues = model.getDiscreteAttribute(commentDimension).getDatas();

		// init hyper-parameters
		lambda = configuration.getFloat("rec.regularization.lambda", 0.001F);
		lambdaU = configuration.getFloat("rec.regularization.lambdaU", 0.001F);
		lambdaV = configuration.getFloat("rec.regularization.lambdaV", 0.001F);
		lambdaB = configuration.getFloat("rec.regularization.lambdaB", 0.001F);
		numberOfTopics = configuration.getInteger("rec.topic.number", 10);
		learnRate = configuration.getFloat("rec.iterator.learnrate", 0.01F);
		numberOfEpoches = configuration.getInteger("rec.iterator.maximum", 10);

		numberOfDocuments = trainMatrix.getElementSize();

		// count the number of words, build the word dictionary and
		// userItemToDoc dictionary
		Map<String, Integer> wordDictionaries = new HashMap<>();
		Table<Integer, Integer, Float> documentTable = HashBasedTable.create();
		int rowCount = 0;
		userItemToDocument = HashBasedTable.create();
		for (DataSample sample : marker) {
			int userIndex = sample.getDiscreteFeature(userDimension);
			int itemIndex = sample.getDiscreteFeature(itemDimension);
			int documentIndex = sample.getDiscreteFeature(commentDimension);
			userItemToDocument.put(userIndex, itemIndex, rowCount);
			// convert wordIds to wordIndices
			String data = (String) documentValues[documentIndex];
			String[] words = data.isEmpty() ? new String[0] : data.split(":");
			for (String word : words) {
				Integer wordIndex = wordDictionaries.get(word);
				if (wordIndex == null) {
					wordIndex = numberOfWords++;
					wordDictionaries.put(word, wordIndex);
				}
				Float oldValue = documentTable.get(rowCount, wordIndex);
				if (oldValue == null) {
					oldValue = 0F;
				}
				float newValue = oldValue + 1F / words.length;
				documentTable.put(rowCount, wordIndex, newValue);
			}
			rowCount++;
		}
		// build W
		W = SparseMatrix.valueOf(numberOfDocuments, numberOfWords, documentTable);

		// init parameters
		initMean = configuration.getFloat("rec.init.mean", 0.0F);
		initStd = configuration.getFloat("rec.init.std", 0.01F);
		userBiases = DenseVector.valueOf(numberOfUsers);
		userBiases.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(distribution.sample().floatValue());
		});
		itemBiases = DenseVector.valueOf(numberOfItems);
		itemBiases.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(distribution.sample().floatValue());
		});
		userFactors = DenseMatrix.valueOf(numberOfUsers, numberOfTopics);
		userFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(distribution.sample().floatValue());
		});
		itemFactors = DenseMatrix.valueOf(numberOfItems, numberOfTopics);
		itemFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(distribution.sample().floatValue());
		});
		K = initStd;

		topicVector = DenseVector.valueOf(numberOfTopics);
		function = new SoftMaxActivationFunction();

		// init theta and phi
		// TODO theta实际是documentFactors
		documentFactors = DenseMatrix.valueOf(numberOfDocuments, numberOfTopics);
		calculateTheta();
		// TODO phi实际是wordFactors
		wordFactors = DenseMatrix.valueOf(numberOfTopics, numberOfWords);
		wordFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(RandomUtility.randomFloat(0.01F));
		});

		logger.info("number of users : " + numberOfUsers);
		logger.info("number of Items : " + numberOfItems);
		logger.info("number of words : " + wordDictionaries.size());
	}

	@Override
	protected void doPractice() {
		DefaultScalar scalar = DefaultScalar.getInstance();
		DenseMatrix transposeThis = DenseMatrix.valueOf(numberOfTopics, numberOfTopics);
		DenseMatrix thetaW = DenseMatrix.valueOf(numberOfTopics, numberOfWords);
		DenseMatrix thetaPhi = DenseMatrix.valueOf(numberOfTopics, numberOfWords);
		for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
			totalLoss = 0F;
			float wordLoss = 0F;
			for (MatrixScalar term : trainMatrix) {
				int userIndex = term.getRow(); // userIdx
				int itemIndex = term.getColumn(); // itemIdx
				int documentIndex = userItemToDocument.get(userIndex, itemIndex);
				float y_true = term.getValue();
				float y_pred = predict(userIndex, itemIndex);

				float error = y_true - y_pred;
				totalLoss += error * error;

				// update user item biases
				float userBiasValue = userBiases.getValue(userIndex);
				userBiases.shiftValue(userIndex, learnRate * (error - lambdaB * userBiasValue));
				totalLoss += lambdaB * userBiasValue * userBiasValue;

				float itemBiasValue = itemBiases.getValue(itemIndex);
				itemBiases.shiftValue(itemIndex, learnRate * (error - lambdaB * itemBiasValue));
				totalLoss += lambdaB * itemBiasValue * itemBiasValue;

				// update user item factors
				for (int factorIndex = 0; factorIndex < numberOfTopics; factorIndex++) {
					float userFactorValue = userFactors.getValue(userIndex, factorIndex);
					float itemFactorValue = itemFactors.getValue(itemIndex, factorIndex);

					userFactors.shiftValue(userIndex, factorIndex, learnRate * (error * itemFactorValue - lambdaU * userFactorValue));
					itemFactors.shiftValue(itemIndex, factorIndex, learnRate * (error * userFactorValue - lambdaV * itemFactorValue));
					totalLoss += lambdaU * userFactorValue * userFactorValue + lambdaV * itemFactorValue * itemFactorValue;

					SparseVector documentVector = W.getRowVector(documentIndex);
					for (VectorScalar documentTerm : documentVector) {
						int wordIndex = documentTerm.getIndex();
						float w_pred = scalar.dotProduct(documentFactors.getRowVector(documentIndex), wordFactors.getColumnVector(wordIndex)).getValue();
						float w_true = documentTerm.getValue();
						float w_error = w_true - w_pred;
						wordLoss += w_error;

						float derivative = 0F;
						for (int topicIndex = 0; topicIndex < numberOfTopics; topicIndex++) {
							if (factorIndex == topicIndex) {
								derivative += w_error * wordFactors.getValue(topicIndex, wordIndex) * documentFactors.getValue(documentIndex, topicIndex) * (1 - documentFactors.getValue(documentIndex, topicIndex));
							} else {
								derivative += w_error * wordFactors.getValue(topicIndex, wordIndex) * documentFactors.getValue(documentIndex, topicIndex) * (-documentFactors.getValue(documentIndex, factorIndex));
							}
							// update K1 K2
							K += learnRate * lambda * w_error * wordFactors.getValue(topicIndex, wordIndex) * documentFactors.getValue(documentIndex, topicIndex) * (1 - documentFactors.getValue(documentIndex, topicIndex)) * Math.abs(userFactors.getValue(userIndex, topicIndex));
						}
						userFactors.shiftValue(userIndex, factorIndex, learnRate * K * derivative * itemFactors.getValue(itemIndex, factorIndex));
						itemFactors.shiftValue(itemIndex, factorIndex, learnRate * K * derivative * userFactors.getValue(userIndex, factorIndex));
					}
				}
			}
			// calculate theta
			logger.info(" iter:" + iterationStep + ", finish factors update");

			// calculate wordLoss and loss
			wordLoss = wordLoss / numberOfTopics;
			totalLoss += wordLoss;
			totalLoss *= 0.5F;
			logger.info(" iter:" + iterationStep + ", loss:" + totalLoss + ", wordLoss:" + wordLoss / 2F);

			calculateTheta();
			logger.info(" iter:" + iterationStep + ", finish theta update");

			// update phi by NMF
			// TODO 此处操作可以整合
			thetaW.dotProduct(documentFactors, true, W, false, MathCalculator.SERIAL);
			transposeThis.dotProduct(documentFactors, true, documentFactors, false, MathCalculator.SERIAL);
			thetaPhi.dotProduct(transposeThis, false, wordFactors, false, MathCalculator.SERIAL);
			for (int topicIndex = 0; topicIndex < numberOfTopics; topicIndex++) {
				for (int wordIndex = 0; wordIndex < numberOfWords; wordIndex++) {
					float numerator = wordFactors.getValue(topicIndex, wordIndex) * thetaW.getValue(topicIndex, wordIndex);
					float denominator = thetaPhi.getValue(topicIndex, wordIndex);
					wordFactors.setValue(topicIndex, wordIndex, numerator / denominator);
				}
			}
			logger.info(" iter:" + iterationStep + ", finish phi update");
		}
	}

	@Override
	protected float predict(int userIndex, int itemIndex) {
		DefaultScalar scalar = DefaultScalar.getInstance();
		float value = meanOfScore + userBiases.getValue(userIndex) + itemBiases.getValue(itemIndex);
		value += scalar.dotProduct(userFactors.getRowVector(userIndex), itemFactors.getRowVector(itemIndex)).getValue();
		return value;
	}

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		int userIndex = dicreteFeatures[userDimension];
		int itemIndex = dicreteFeatures[itemDimension];
		return predict(userIndex, itemIndex);
	}

	/**
	 * Calculate theta vectors via userFactors and itemFactors. thetaVector =
	 * softmax( exp(K|u||v|) )
	 */
	private void calculateTheta() {
		for (MatrixScalar term : trainMatrix) {
			int userIndex = term.getRow();
			int itemIndex = term.getColumn();
			int documentIdx = userItemToDocument.get(userIndex, itemIndex);
			DenseVector documentVector = documentFactors.getRowVector(documentIdx);
			topicVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
				int index = scalar.getIndex();
				float value = scalar.getValue();
				value = K * Math.abs(userFactors.getValue(userIndex, index)) * Math.abs(itemFactors.getValue(itemIndex, index));
				scalar.setValue(value);
			});
			function.forward(topicVector, documentVector);
		}
	}

}
