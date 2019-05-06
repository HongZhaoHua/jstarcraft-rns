package com.jstarcraft.recommendation.recommender.collaborative.rating;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.recommendation.configurator.Configuration;
import com.jstarcraft.recommendation.recommender.MatrixFactorizationRecommender;

/**
 * 
 * LLORMA推荐器
 * 
 * <pre>
 * Local Low-Rank Matrix Approximation
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class LLORMARecommender extends MatrixFactorizationRecommender {
	private int numberOfGlobalFactors, numberOfLocalFactors;
	private int numberOfGlobalIterations, numberOfLocalIterations;
	private int numberOfThreads;
	private float globalUserRegularization, globalItemRegularization, localUserRegularization, localItemRegularization;
	private float globalLearnRate, localLearnRate;

	private int numberOfModels;
	private DenseMatrix globalUserFactors, globalItemFactors;

	private DenseMatrix[] userMatrixes;

	private DenseMatrix[] itemMatrixes;

	private int[] anchorUsers;
	private int[] anchorItems;

	/*
	 * (non-Javadoc)
	 *
	 * @see net.librec.recommender.AbstractRecommender#setup()
	 */
	@Override
	public void prepare(Configuration configuration, DataModule model, DataSpace space) {
		super.prepare(configuration, model, space);
		numberOfGlobalFactors = configuration.getInteger("rec.global.factors.num", 20);
		numberOfLocalFactors = numberOfFactors;

		numberOfGlobalIterations = configuration.getInteger("rec.global.iteration.maximum", 100);
		numberOfLocalIterations = numberOfEpoches;

		globalUserRegularization = configuration.getFloat("rec.global.user.regularization", 0.01F);
		globalItemRegularization = configuration.getFloat("rec.global.item.regularization", 0.01F);
		localUserRegularization = userRegularization;
		localItemRegularization = itemRegularization;

		globalLearnRate = configuration.getFloat("rec.global.iteration.learnrate", 0.01F);
		localLearnRate = configuration.getFloat("rec.iteration.learnrate", 0.01F);

		numberOfThreads = configuration.getInteger("rec.thread.count", 4);
		numberOfModels = configuration.getInteger("rec.model.num", 50);

		numberOfThreads = numberOfThreads > numberOfModels ? numberOfModels : numberOfThreads;

		// global svd P Q to calculate the kernel value between users (or items)
		globalUserFactors = DenseMatrix.valueOf(numberOfUsers, numberOfGlobalFactors);
		globalUserFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(distribution.sample().floatValue());
		});
		globalItemFactors = DenseMatrix.valueOf(numberOfItems, numberOfGlobalFactors);
		globalItemFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(distribution.sample().floatValue());
		});
	}

	// global svd P Q
	private void practiceGlobalModel(DefaultScalar scalar) {
		for (int iterationStep = 1; iterationStep <= numberOfGlobalIterations; iterationStep++) {
			for (MatrixScalar term : trainMatrix) {
				int userIndex = term.getRow(); // user
				int itemIndex = term.getColumn(); // item
				float rate = term.getValue();

				// TODO 考虑重构,减少userVector与itemVector的重复构建
				DenseVector userVector = globalUserFactors.getRowVector(userIndex);
				DenseVector itemVector = globalItemFactors.getRowVector(itemIndex);
				float predict = scalar.dotProduct(userVector, itemVector).getValue();
				float error = rate - predict;

				// update factors
				for (int factorIndex = 0; factorIndex < numberOfGlobalFactors; factorIndex++) {
					float userFactor = globalUserFactors.getValue(userIndex, factorIndex);
					float itemFactor = globalItemFactors.getValue(itemIndex, factorIndex);
					globalUserFactors.shiftValue(userIndex, factorIndex, globalLearnRate * (error * itemFactor - globalUserRegularization * userFactor));
					globalItemFactors.shiftValue(itemIndex, factorIndex, globalLearnRate * (error * userFactor - globalItemRegularization * itemFactor));
				}
			}
		}

		userMatrixes = new DenseMatrix[numberOfModels];
		itemMatrixes = new DenseMatrix[numberOfModels];
		anchorUsers = new int[numberOfModels];
		anchorItems = new int[numberOfModels];
		// end of training
	}

	/**
	 * Calculate similarity between two users, based on the global base SVD.
	 *
	 * @param leftUserIndex
	 *            The first user's ID.
	 * @param rightUserIndex
	 *            The second user's ID.
	 * @return The similarity value between two users idx1 and idx2.
	 */
	private float getUserSimilarity(DefaultScalar scalar, int leftUserIndex, int rightUserIndex) {
		float similarity;
		// TODO 减少向量的重复构建
		DenseVector leftUserVector = globalUserFactors.getRowVector(leftUserIndex);
		DenseVector rightUserVector = globalUserFactors.getRowVector(rightUserIndex);
		similarity = (float) (1 - 2F / Math.PI * Math.acos(scalar.dotProduct(leftUserVector, rightUserVector).getValue() / (Math.sqrt(scalar.dotProduct(leftUserVector, leftUserVector).getValue()) * Math.sqrt(scalar.dotProduct(rightUserVector, rightUserVector).getValue()))));
		if (Double.isNaN(similarity)) {
			similarity = 0F;
		}
		return similarity;
	}

	/**
	 * Calculate similarity between two items, based on the global base SVD.
	 *
	 * @param leftItemIndex
	 *            The first item's ID.
	 * @param rightItemIndex
	 *            The second item's ID.
	 * @return The similarity value between two items idx1 and idx2.
	 */
	private float getItemSimilarity(DefaultScalar scalar, int leftItemIndex, int rightItemIndex) {
		float similarity;
		// TODO 减少向量的重复构建
		DenseVector leftItemVector = globalItemFactors.getRowVector(leftItemIndex);
		DenseVector rightItemVector = globalItemFactors.getRowVector(rightItemIndex);
		similarity = (float) (1 - 2D / Math.PI * Math.acos(scalar.dotProduct(leftItemVector, rightItemVector).getValue() / (Math.sqrt(scalar.dotProduct(leftItemVector, leftItemVector).getValue()) * Math.sqrt(scalar.dotProduct(rightItemVector, rightItemVector).getValue()))));
		if (Double.isNaN(similarity)) {
			similarity = 0F;
		}
		return similarity;
	}

	/**
	 * Given the similarity, it applies the given kernel. This is done either for
	 * all users or for all items.
	 *
	 * @param size
	 *            The length of user or item vector.
	 * @param anchorIdx
	 *            The identifier of anchor point.
	 * @param type
	 *            The type of kernel.
	 * @param width
	 *            Kernel width.
	 * @param isItemFeature
	 *            return item kernel if yes, return user kernel otherwise.
	 * @return The kernel-smoothed values for all users or all items.
	 */
	private DenseVector kernelSmoothing(DefaultScalar scalar, int size, int anchorIdx, KernelSmoother type, float width, boolean isItemFeature) {
		DenseVector featureVector = DenseVector.valueOf(size);
		// TODO 此处似乎有Bug?
		featureVector.setValue(anchorIdx, 1F);
		for (int index = 0; index < size; index++) {
			float similarity;
			if (isItemFeature) {
				similarity = getItemSimilarity(scalar, index, anchorIdx);
			} else { // userFeature
				similarity = getUserSimilarity(scalar, index, anchorIdx);
			}
			featureVector.setValue(index, type.kernelize(similarity, width));
		}
		return featureVector;
	}

	private void practiceLocalModels(DefaultScalar scalar) {
		// Pre-calculating similarity:
		int completeModelCount = 0;

		// TODO 此处的变量与矩阵可以整合到LLORMALearner,LLORMALearner变成任务.
		LLORMALearner[] learners = new LLORMALearner[numberOfThreads];

		int modelCount = 0;
		int[] runningThreadList = new int[numberOfThreads];
		int runningThreadCount = 0;
		int waitingThreadPointer = 0;
		int nextRunningSlot = 0;

		// Parallel training:
		while (completeModelCount < numberOfModels) {
			int randomUserIndex = RandomUtility.randomInteger(numberOfUsers);
			// TODO 考虑重构
			SparseVector userVector = trainMatrix.getRowVector(randomUserIndex);
			if (userVector.getElementSize() == 0) {
				continue;
			}
			// TODO 此处的并发模型有问题,需要重构.否则当第一次runningThreadCount >=
			// numThreads之后,都是单线程执行.
			if (runningThreadCount < numberOfThreads && modelCount < numberOfModels) {
				// Selecting a new anchor point:
				int randomItemIndex = userVector.getIndex(RandomUtility.randomInteger(userVector.getElementSize()));
				anchorUsers[modelCount] = randomUserIndex;
				anchorItems[modelCount] = randomItemIndex;
				// Preparing weight vectors:
				DenseVector userWeights = kernelSmoothing(scalar, numberOfUsers, randomUserIndex, KernelSmoother.EPANECHNIKOV_KERNEL, 0.8F, false);
				DenseVector itemWeights = kernelSmoothing(scalar, numberOfItems, randomItemIndex, KernelSmoother.EPANECHNIKOV_KERNEL, 0.8F, true);
				DenseMatrix localUserFactors = DenseMatrix.valueOf(numberOfUsers, numberOfLocalFactors);
				localUserFactors.iterateElement(MathCalculator.SERIAL, (element) -> {
					element.setValue(distribution.sample().floatValue());
				});
				DenseMatrix localItemFactors = DenseMatrix.valueOf(numberOfItems, numberOfLocalFactors);
				localItemFactors.iterateElement(MathCalculator.SERIAL, (element) -> {
					element.setValue(distribution.sample().floatValue());
				});
				// Starting a new local model learning:
				learners[nextRunningSlot] = new LLORMALearner(modelCount, numberOfLocalFactors, localLearnRate, localUserRegularization, localItemRegularization, numberOfLocalIterations, localUserFactors, localItemFactors, userWeights, itemWeights, trainMatrix);
				learners[nextRunningSlot].start();
				runningThreadList[runningThreadCount] = modelCount;
				runningThreadCount++;
				modelCount++;
				nextRunningSlot++;
			} else if (runningThreadCount > 0) {
				// Joining a local model which was done with learning:
				try {
					learners[waitingThreadPointer].join();
				} catch (InterruptedException ie) {
					logger.error("Join failed: " + ie);
				}
				LLORMALearner learner = learners[waitingThreadPointer];
				userMatrixes[learner.getIndex()] = learner.getUserFactors();
				itemMatrixes[learner.getIndex()] = learner.getItemFactors();
				nextRunningSlot = waitingThreadPointer;
				waitingThreadPointer = (waitingThreadPointer + 1) % numberOfThreads;
				runningThreadCount--;
				completeModelCount++;
			}
		}
	}

	@Override
	protected void doPractice() {
		DefaultScalar scalar = DefaultScalar.getInstance();
		practiceGlobalModel(scalar);
		practiceLocalModels(scalar);
	}

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		DefaultScalar scalar = DefaultScalar.getInstance();
		int userIndex = dicreteFeatures[userDimension];
		int itemIndex = dicreteFeatures[itemDimension];
		float weightSum = 0;
		float valueSum = 0F;
		for (int iterationStep = 0; iterationStep < numberOfModels; iterationStep++) {
			float weight = KernelSmoother.EPANECHNIKOV_KERNEL.kernelize(getUserSimilarity(scalar, anchorUsers[iterationStep], userIndex), 0.8F) * KernelSmoother.EPANECHNIKOV_KERNEL.kernelize(getItemSimilarity(scalar, anchorItems[iterationStep], itemIndex), 0.8F);
			float value = (scalar.dotProduct(userMatrixes[iterationStep].getRowVector(userIndex), itemMatrixes[iterationStep].getRowVector(itemIndex)).getValue()) * weight;
			weightSum += weight;
			valueSum += value;
		}
		float score = valueSum / weightSum;
		if (Float.isNaN(score) || score == 0F) {
			score = meanOfScore;
		} else if (score < minimumOfScore) {
			score = minimumOfScore;
		} else if (score > maximumOfScore) {
			score = maximumOfScore;
		}
		return score;
	}

}
