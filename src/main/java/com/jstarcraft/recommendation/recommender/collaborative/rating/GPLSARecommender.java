package com.jstarcraft.recommendation.recommender.collaborative.rating;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.ai.utility.Float2FloatKeyValue;
import com.jstarcraft.ai.utility.MathUtility;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.recommendation.configurator.Configuration;
import com.jstarcraft.recommendation.recommender.ProbabilisticGraphicalRecommender;
import com.jstarcraft.recommendation.utility.GaussianUtility;

/**
 * 
 * GPLSA推荐器
 * 
 * <pre>
 * Collaborative Filtering via Gaussian Probabilistic Latent Semantic Analysis
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class GPLSARecommender extends ProbabilisticGraphicalRecommender {

	/*
	 * {user, item, {topic z, probability}}
	 */
	protected Table<Integer, Integer, float[]> probabilityTensor;
	/*
	 * Conditional Probability: P(z|u)
	 */
	protected DenseMatrix userTopicProbabilities;
	/*
	 * Conditional Probability: P(v|y,z)
	 */
	protected DenseMatrix itemMus, itemSigmas;
	/*
	 * regularize ratings
	 */
	protected DenseVector userMus, userSigmas;
	/*
	 * smoothing weight
	 */
	protected float smoothWeight;
	/*
	 * tempered EM parameter beta, suggested by Wu Bin
	 */
	protected float beta;
	/*
	 * small value for initialization
	 */
	protected static float smallValue = MathUtility.EPSILON;

	@Override
	public void prepare(Configuration configuration, DataModule model, DataSpace space) {
		super.prepare(configuration, model, space);
		// Initialize users' conditional probabilities
		userTopicProbabilities = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
		for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
			DenseVector probabilityVector = userTopicProbabilities.getRowVector(userIndex);
			probabilityVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
				scalar.setValue(RandomUtility.randomInteger(numberOfFactors) + 1);
			});
			probabilityVector.scaleValues(1F / probabilityVector.getSum(false));
		}

		Float2FloatKeyValue keyValue = trainMatrix.getVariance();
		float mean = keyValue.getKey();
		float variance = keyValue.getValue() / trainMatrix.getElementSize();

		userMus = DenseVector.valueOf(numberOfUsers);
		userSigmas = DenseVector.valueOf(numberOfUsers);
		smoothWeight = configuration.getInteger("rec.recommender.smoothWeight");
		for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
			SparseVector userVector = trainMatrix.getRowVector(userIndex);
			int size = userVector.getElementSize();
			if (size < 1) {
				continue;
			}
			float mu = (userVector.getSum(false) + smoothWeight * mean) / (size + smoothWeight);
			userMus.setValue(userIndex, mu);
			float sigma = userVector.getVariance(mu);
			sigma += smoothWeight * variance;
			sigma = (float) Math.sqrt(sigma / (size + smoothWeight));
			userSigmas.setValue(userIndex, sigma);
		}

		// Initialize Q
		// TODO 重构
		probabilityTensor = HashBasedTable.create();

		for (MatrixScalar term : trainMatrix) {
			int userIndex = term.getRow();
			int itemIndex = term.getColumn();
			float rate = term.getValue();
			rate = (rate - userMus.getValue(userIndex)) / userSigmas.getValue(userIndex);
			term.setValue(rate);
			probabilityTensor.put(userIndex, itemIndex, new float[numberOfFactors]);
		}

		itemMus = DenseMatrix.valueOf(numberOfItems, numberOfFactors);
		itemSigmas = DenseMatrix.valueOf(numberOfItems, numberOfFactors);
		for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
			SparseVector itemVector = trainMatrix.getColumnVector(itemIndex);
			int size = itemVector.getElementSize();
			if (size < 1) {
				continue;
			}
			float mu = itemVector.getSum(false) / size;
			float sigma = itemVector.getVariance(mu);
			sigma = (float) Math.sqrt(sigma / size);
			for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
				itemMus.setValue(itemIndex, topicIndex, mu + smallValue * RandomUtility.randomFloat(1F));
				itemSigmas.setValue(itemIndex, topicIndex, sigma + smallValue * RandomUtility.randomFloat(1F));
			}
		}
	}

	@Override
	protected void eStep() {
		// variational inference to compute Q
		float[] numerators = new float[numberOfFactors];
		for (MatrixScalar term : trainMatrix) {
			int userIndex = term.getRow();
			int itemIndex = term.getColumn();
			float rate = term.getValue();
			float denominator = 0F;
			for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
				float pdf = GaussianUtility.probabilityDensity(rate, itemMus.getValue(itemIndex, topicIndex), itemSigmas.getValue(itemIndex, topicIndex));
				float value = (float) Math.pow(userTopicProbabilities.getValue(userIndex, topicIndex) * pdf, beta); // Tempered
				// EM
				numerators[topicIndex] = value;
				denominator += value;
			}
			float[] probabilities = probabilityTensor.get(userIndex, itemIndex);
			for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
				float probability = (denominator > 0 ? numerators[topicIndex] / denominator : 0);
				probabilities[topicIndex] = probability;
			}
		}
	}

	@Override
	protected void mStep() {
		float[] numerators = new float[numberOfFactors];
		// theta_u,z
		for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
			SparseVector userVector = trainMatrix.getRowVector(userIndex);
			if (userVector.getElementSize() < 1) {
				continue;
			}
			float denominator = 0F;
			for (VectorScalar term : userVector) {
				int itemIndex = term.getIndex();
				float[] probabilities = probabilityTensor.get(userIndex, itemIndex);
				for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
					numerators[topicIndex] = probabilities[topicIndex];
					denominator += numerators[topicIndex];
				}
			}
			for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
				userTopicProbabilities.setValue(userIndex, topicIndex, numerators[topicIndex] / denominator);
			}
		}

		// topicItemMu, topicItemSigma
		for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
			SparseVector itemVector = trainMatrix.getColumnVector(itemIndex);
			if (itemVector.getElementSize() < 1) {
				continue;
			}
			float numerator = 0F, denominator = 0F;
			for (VectorScalar term : itemVector) {
				int userIndex = term.getIndex();
				float rate = term.getValue();
				float[] probabilities = probabilityTensor.get(userIndex, itemIndex);
				for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
					float probability = probabilities[topicIndex];
					numerator += rate * probability;
					denominator += probability;
				}
			}
			float mu = denominator > 0F ? numerator / denominator : 0F;
			numerator = 0F;
			for (VectorScalar term : itemVector) {
				int userIndex = term.getIndex();
				float rate = term.getValue();
				float[] probabilities = probabilityTensor.get(userIndex, itemIndex);
				for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
					double probability = probabilities[topicIndex];
					numerator += Math.pow(rate - mu, 2) * probability;
				}
			}
			float sigma = (float) (denominator > 0F ? Math.sqrt(numerator / denominator) : 0F);
			for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
				itemMus.setValue(itemIndex, topicIndex, mu);
				itemSigmas.setValue(itemIndex, topicIndex, sigma);
			}
		}
	}

	@Override
	public float predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
		float sum = 0F;
		for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
			sum += userTopicProbabilities.getValue(userIndex, topicIndex) * itemMus.getValue(itemIndex, topicIndex);
		}
		return userMus.getValue(userIndex) + userSigmas.getValue(userIndex) * sum;
	}

}
