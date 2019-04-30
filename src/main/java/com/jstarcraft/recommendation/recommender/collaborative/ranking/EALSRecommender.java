package com.jstarcraft.recommendation.recommender.collaborative.ranking;

import java.util.concurrent.CountDownLatch;

import com.jstarcraft.ai.environment.EnvironmentContext;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.DenseModule;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;
import com.jstarcraft.recommendation.exception.RecommendationException;
import com.jstarcraft.recommendation.recommender.MatrixFactorizationRecommender;

/**
 * 
 * EALS推荐器
 * 
 * <pre>
 * EALS: efficient Alternating Least Square for Weighted Regularized Matrix Factorization
 * Collaborative filtering for implicit feedback dataset
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class EALSRecommender extends MatrixFactorizationRecommender {
	/**
	 * confidence weight coefficient for WRMF
	 */
	protected float weightCoefficient;

	/**
	 * the significance level of popular items over un-popular ones
	 */
	private float ratio;

	/**
	 * the overall weight of missing data c0
	 */
	private float overallWeight;

	/**
	 * 0：eALS MF; 1：WRMF; 2: both
	 */
	private int type;

	/**
	 * confidence that item i missed by users
	 */
	private float[] confidences;

	/**
	 * weights of all user-item pair (u,i)
	 */
	private SparseMatrix weights;

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, DenseModule model, DataSpace space) {
		super.prepare(configuration, marker, model, space);
		weightCoefficient = configuration.getFloat("rec.wrmf.weight.coefficient", 4.0f);
		ratio = configuration.getFloat("rec.eals.ratio", 0.4f);
		overallWeight = configuration.getFloat("rec.eals.overall", 128.0f);
		type = configuration.getInteger("rec.eals.wrmf.judge", 1);

		confidences = new float[numberOfItems];

		// get ci
		if (type == 0 || type == 2) {
			float sumPopularity = 0F;
			for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
				float alphaPopularity = (float) Math.pow(trainMatrix.getColumnScope(itemIndex) * 1.0 / numberOfActions, ratio);
				confidences[itemIndex] = overallWeight * alphaPopularity;
				sumPopularity += alphaPopularity;
			}
			for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
				confidences[itemIndex] = confidences[itemIndex] / sumPopularity;
			}
		} else {
			for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
				confidences[itemIndex] = 1;
			}
		}

		weights = SparseMatrix.copyOf(trainMatrix, false);
		weights.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			if (type == 1 || type == 2) {
				scalar.setValue(1F + weightCoefficient * scalar.getValue());
			} else {
				scalar.setValue(1F);
			}
		});
	}

	private ThreadLocal<float[]> itemScoreStorage = new ThreadLocal<>();
	private ThreadLocal<float[]> itemWeightStorage = new ThreadLocal<>();
	private ThreadLocal<float[]> userScoreStorage = new ThreadLocal<>();
	private ThreadLocal<float[]> userWeightStorage = new ThreadLocal<>();

	@Override
	protected void constructEnvironment() {
		// TODO 可以继续节省数组分配的大小(按照稀疏矩阵的最大向量作为缓存大小).
		itemScoreStorage.set(new float[numberOfItems]);
		itemWeightStorage.set(new float[numberOfItems]);
		userScoreStorage.set(new float[numberOfUsers]);
		userWeightStorage.set(new float[numberOfUsers]);
	}

	@Override
	protected void destructEnvironment() {
		itemScoreStorage.remove();
		itemWeightStorage.remove();
		userScoreStorage.remove();
		userWeightStorage.remove();
	}

	@Override
	protected void doPractice() {
		EnvironmentContext context = EnvironmentContext.getContext();
		DenseMatrix itemDeltas = DenseMatrix.valueOf(numberOfFactors, numberOfFactors);
		DenseMatrix userDeltas = DenseMatrix.valueOf(numberOfFactors, numberOfFactors);

		for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
			// Update the Sq cache
			for (int leftFactorIndex = 0; leftFactorIndex < numberOfFactors; leftFactorIndex++) {
				for (int rightFactorIndex = leftFactorIndex; rightFactorIndex < numberOfFactors; rightFactorIndex++) {
					float value = 0F;
					for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
						value += confidences[itemIndex] * itemFactors.getValue(itemIndex, leftFactorIndex) * itemFactors.getValue(itemIndex, rightFactorIndex);
					}
					itemDeltas.setValue(leftFactorIndex, rightFactorIndex, value);
					itemDeltas.setValue(rightFactorIndex, leftFactorIndex, value);
				}
			}
			// Step 1: update user factors;
			// 按照用户切割任务实现并发计算.
			CountDownLatch userLatch = new CountDownLatch(numberOfUsers);
			for (int index = 0; index < numberOfUsers; index++) {
				int userIndex = index;
				context.doAlgorithmByAny(index, () -> {
					DefaultScalar scalar = DefaultScalar.getInstance();
					SparseVector userVector = weights.getRowVector(userIndex);
					DenseVector factorVector = userFactors.getRowVector(userIndex);
					float[] itemScores = itemScoreStorage.get();
					float[] itemWeights = itemWeightStorage.get();
					for (VectorScalar term : userVector) {
						int itemIndex = term.getIndex();
						DenseVector itemVector = itemFactors.getRowVector(itemIndex);
						itemScores[itemIndex] = scalar.dotProduct(itemVector, factorVector).getValue();
						itemWeights[itemIndex] = term.getValue();
					}
					for (int leftFactorIndex = 0; leftFactorIndex < numberOfFactors; leftFactorIndex++) {
						float numerator = 0, denominator = userRegularization + itemDeltas.getValue(leftFactorIndex, leftFactorIndex);
						for (int rightFactorIndex = 0; rightFactorIndex < numberOfFactors; rightFactorIndex++) {
							if (leftFactorIndex != rightFactorIndex) {
								numerator -= userFactors.getValue(userIndex, rightFactorIndex) * itemDeltas.getValue(leftFactorIndex, rightFactorIndex);
							}
						}
						for (VectorScalar term : userVector) {
							int itemIndex = term.getIndex();
							itemScores[itemIndex] -= userFactors.getValue(userIndex, leftFactorIndex) * itemFactors.getValue(itemIndex, leftFactorIndex);
							numerator += (itemWeights[itemIndex] - (itemWeights[itemIndex] - confidences[itemIndex]) * itemScores[itemIndex]) * itemFactors.getValue(itemIndex, leftFactorIndex);
							denominator += (itemWeights[itemIndex] - confidences[itemIndex]) * itemFactors.getValue(itemIndex, leftFactorIndex) * itemFactors.getValue(itemIndex, leftFactorIndex);
						}
						// update puf
						userFactors.setValue(userIndex, leftFactorIndex, numerator / denominator);
						for (VectorScalar term : userVector) {
							int itemIndex = term.getIndex();
							itemScores[itemIndex] += userFactors.getValue(userIndex, leftFactorIndex) * itemFactors.getValue(itemIndex, leftFactorIndex);
						}
					}
					userLatch.countDown();
				});
			}
			try {
				userLatch.await();
			} catch (Exception exception) {
				throw new RecommendationException(exception);
			}

			// Update the Sp cache
			userDeltas.dotProduct(userFactors, true, userFactors, false, MathCalculator.SERIAL);
			// Step 2: update item factors;
			// 按照物品切割任务实现并发计算.
			CountDownLatch itemLatch = new CountDownLatch(numberOfItems);
			for (int index = 0; index < numberOfItems; index++) {
				int itemIndex = index;
				context.doAlgorithmByAny(index, () -> {
					DefaultScalar scalar = DefaultScalar.getInstance();
					SparseVector itemVector = weights.getColumnVector(itemIndex);
					DenseVector factorVector = itemFactors.getRowVector(itemIndex);
					float[] userScores = userScoreStorage.get();
					float[] userWeights = userWeightStorage.get();
					for (VectorScalar term : itemVector) {
						int userIndex = term.getIndex();
						DenseVector userVector = userFactors.getRowVector(userIndex);
						userScores[userIndex] = scalar.dotProduct(userVector, factorVector).getValue();
						userWeights[userIndex] = term.getValue();
					}
					for (int leftFactorIndex = 0; leftFactorIndex < numberOfFactors; leftFactorIndex++) {
						float numerator = 0, denominator = confidences[itemIndex] * userDeltas.getValue(leftFactorIndex, leftFactorIndex) + itemRegularization;
						for (int rightFactorIndex = 0; rightFactorIndex < numberOfFactors; rightFactorIndex++) {
							if (leftFactorIndex != rightFactorIndex) {
								numerator -= itemFactors.getValue(itemIndex, rightFactorIndex) * userDeltas.getValue(rightFactorIndex, leftFactorIndex);
							}
						}
						numerator *= confidences[itemIndex];
						for (VectorScalar term : itemVector) {
							int userIndex = term.getIndex();
							userScores[userIndex] -= userFactors.getValue(userIndex, leftFactorIndex) * itemFactors.getValue(itemIndex, leftFactorIndex);
							numerator += (userWeights[userIndex] - (userWeights[userIndex] - confidences[itemIndex]) * userScores[userIndex]) * userFactors.getValue(userIndex, leftFactorIndex);
							denominator += (userWeights[userIndex] - confidences[itemIndex]) * userFactors.getValue(userIndex, leftFactorIndex) * userFactors.getValue(userIndex, leftFactorIndex);
						}
						// update qif
						itemFactors.setValue(itemIndex, leftFactorIndex, numerator / denominator);
						for (VectorScalar term : itemVector) {
							int userIndex = term.getIndex();
							userScores[userIndex] += userFactors.getValue(userIndex, leftFactorIndex) * itemFactors.getValue(itemIndex, leftFactorIndex);
						}
					}
					itemLatch.countDown();
				});
			}
			try {
				itemLatch.await();
			} catch (Exception exception) {
				throw new RecommendationException(exception);
			}
		}
	}

}
