package com.jstarcraft.rns.recommender.collaborative.rating;

import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;

/**
 * 
 * LLORMA学习器
 * 
 * <pre>
 * Local Low-Rank Matrix Approximation
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class LLORMALearner extends Thread {
	/**
	 * The unique identifier of the thread.
	 */
	private int threadId;

	/**
	 * The number of features.
	 */
	private int numberOfFactors;

	/**
	 * Learning rate parameter.
	 */
	private float learnRatio;

	/**
	 * The maximum number of iteration.
	 */
	private int localIteration;

	/**
	 * Regularization factor parameter.
	 */
	private float userRegularization, itemRegularization;

	/**
	 * User profile in low-rank matrix form.
	 */
	private DenseMatrix userFactors;

	/**
	 * Item profile in low-rank matrix form.
	 */
	private DenseMatrix itemFactors;

	/**
	 * The vector containing each user's weight.
	 */
	private DenseVector userWeights;

	/**
	 * The vector containing each item's weight.
	 */
	private DenseVector itemWeights;

	/**
	 * The rating matrix used for learning.
	 */
	private SparseMatrix trainMatrix;

	/**
	 * Construct a local model for singleton LLORMA.
	 *
	 * @param threadId
	 *            A unique thread ID.
	 * @param numberOfFactors
	 *            The rank which will be used in this local model.
	 * @param numUsersParam
	 *            The number of users.
	 * @param numItemsParam
	 *            The number of items.
	 * @param anchorUserParam
	 *            The anchor user used to learn this local model.
	 * @param anchorItemParam
	 *            The anchor item used to learn this local model.
	 * @param learnRatio
	 *            Learning rate parameter.
	 * @param userWeights
	 *            Initial vector containing each user's weight.
	 * @param itemWeights
	 *            Initial vector containing each item's weight.
	 * @param trainMatrix
	 *            The rating matrix used for learning.
	 * @param localIteration
	 *            localIterationParam
	 * @param itemRegularization
	 *            localRegItemParam
	 * @param userRegularization
	 *            localRegUserParam
	 */
	public LLORMALearner(int threadId, int numberOfFactors, float learnRatio, float userRegularization, float itemRegularization, int localIteration, DenseMatrix userFactors, DenseMatrix itemFactors, DenseVector userWeights, DenseVector itemWeights, SparseMatrix trainMatrix) {
		this.threadId = threadId;
		this.numberOfFactors = numberOfFactors;
		this.learnRatio = learnRatio;
		this.userRegularization = userRegularization;
		this.itemRegularization = itemRegularization;
		this.localIteration = localIteration;
		this.userWeights = userWeights;
		this.itemWeights = itemWeights;
		this.userFactors = userFactors;
		this.itemFactors = itemFactors;
		this.trainMatrix = trainMatrix;
	}

	public int getIndex() {
		return threadId;
	}

	/**
	 * Getter method for user profile of this local model.
	 *
	 * @return The user profile of this local model.
	 */
	public DenseMatrix getUserFactors() {
		return userFactors;
	}

	/**
	 * Getter method for item profile of this local model.
	 *
	 * @return The item profile of this local model.
	 */
	public DenseMatrix getItemFactors() {
		return itemFactors;
	}

	/**
	 * Learn this local model based on similar users to the anchor user and similar
	 * items to the anchor item. Implemented with gradient descent.
	 */
	@Override
	public void run() {
		// Learn by Weighted RegSVD
		for (int iterationStep = 0; iterationStep < localIteration; iterationStep++) {
			for (MatrixScalar term : trainMatrix) {
				int userIndex = term.getRow(); // user
				int itemIndex = term.getColumn(); // item
				float rate = term.getValue();

				float predict = predict(userIndex, itemIndex);
				float error = rate - predict;
				float weight = userWeights.getValue(userIndex) * itemWeights.getValue(itemIndex);

				// update factors
				for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
					float userFactorValue = userFactors.getValue(userIndex, factorIndex);
					float itemFactorValue = itemFactors.getValue(itemIndex, factorIndex);
					userFactors.shiftValue(userIndex, factorIndex, learnRatio * (error * itemFactorValue * weight - userRegularization * userFactorValue));
					itemFactors.shiftValue(itemIndex, factorIndex, learnRatio * (error * userFactorValue * weight - itemRegularization * itemFactorValue));
				}
			}
		}
	}

	private float predict(int userIndex, int itemIndex) {
		DefaultScalar scalar = DefaultScalar.getInstance();
		DenseVector userVector = userFactors.getRowVector(userIndex);
		DenseVector itemVector = itemFactors.getRowVector(itemIndex);
		float value = scalar.dotProduct(userVector, itemVector).getValue();
		return value;
	}

}
