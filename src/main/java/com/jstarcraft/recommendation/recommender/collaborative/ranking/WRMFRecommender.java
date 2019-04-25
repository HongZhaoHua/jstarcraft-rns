package com.jstarcraft.recommendation.recommender.collaborative.ranking;

import java.util.Date;
import java.util.concurrent.CountDownLatch;

import com.jstarcraft.ai.environment.EnvironmentContext;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.InstanceAccessor;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;
import com.jstarcraft.recommendation.exception.RecommendationException;
import com.jstarcraft.recommendation.recommender.MatrixFactorizationRecommender;
import com.jstarcraft.recommendation.utility.MatrixUtility;

/**
 * 
 * WRMF推荐器
 * 
 * <pre>
 * WRMF: Weighted Regularized Matrix Factorization
 * Collaborative filtering for implicit feedback datasets
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class WRMFRecommender extends MatrixFactorizationRecommender {
	/**
	 * confidence weight coefficient
	 */
	private float weightCoefficient;

	/**
	 * confindence Minus Identity Matrix{ui} = confidenceMatrix_{ui} - 1 =alpha *
	 * r_{ui} or log(1+10^alpha * r_{ui})
	 */
	// TODO 应该重构为SparseMatrix
	private SparseMatrix confindenceMatrix;

	/**
	 * preferenceMatrix_{ui} = 1 if {@code r_{ui}>0 or preferenceMatrix_{ui} = 0}
	 */
	// TODO 应该重构为SparseMatrix
	private SparseMatrix preferenceMatrix;

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, InstanceAccessor model, DataSpace space) {
		super.prepare(configuration, marker, model, space);
		weightCoefficient = configuration.getFloat("rec.wrmf.weight.coefficient", 4.0f);

		confindenceMatrix = SparseMatrix.copyOf(trainMatrix, false);
		confindenceMatrix.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue((float) Math.log(1F + Math.pow(10, weightCoefficient) * scalar.getValue()));
		});
		preferenceMatrix = SparseMatrix.copyOf(trainMatrix, false);
		preferenceMatrix.setValues(1F);
	}

	private ThreadLocal<DenseMatrix> factorMatrixStorage = new ThreadLocal<>();
	private ThreadLocal<DenseMatrix> copyMatrixStorage = new ThreadLocal<>();
	private ThreadLocal<DenseMatrix> inverseMatrixStorage = new ThreadLocal<>();

	@Override
	protected void constructEnvironment() {
		// 缓存特征计算,避免消耗内存
		factorMatrixStorage.set(DenseMatrix.valueOf(numberOfFactors, numberOfFactors));
		copyMatrixStorage.set(DenseMatrix.valueOf(numberOfFactors, numberOfFactors));
		inverseMatrixStorage.set(DenseMatrix.valueOf(numberOfFactors, numberOfFactors));
	}

	@Override
	protected void destructEnvironment() {
		factorMatrixStorage.remove();
		copyMatrixStorage.remove();
		inverseMatrixStorage.remove();
	}

	@Override
	protected void doPractice() {
		EnvironmentContext context = EnvironmentContext.getContext();
		// 缓存特征计算,避免消耗内存
		DenseMatrix transposeMatrix = DenseMatrix.valueOf(numberOfFactors, numberOfFactors);

		// To be consistent with the symbols in the paper
		// Updating by using alternative least square (ALS)
		// due to large amount of entries to be processed (SGD will be too slow)
		for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
			// Step 1: update user factors;
			// 按照用户切割任务实现并发计算.
			DenseMatrix itemSymmetryMatrix = transposeMatrix;
			itemSymmetryMatrix.dotProduct(itemFactors, true, itemFactors, false, MathCalculator.SERIAL);
			CountDownLatch userLatch = new CountDownLatch(numberOfUsers);
			for (int index = 0; index < numberOfUsers; index++) {
				int userIndex = index;
				context.doAlgorithmByAny(index, () -> {
					DenseMatrix factorMatrix = factorMatrixStorage.get();
					DenseMatrix copyMatrix = copyMatrixStorage.get();
					DenseMatrix inverseMatrix = inverseMatrixStorage.get();
					SparseVector confindenceVector = confindenceMatrix.getRowVector(userIndex);
					// YtY + Yt * (Cu - itemIdx) * Y
					factorMatrix.iterateElement(MathCalculator.SERIAL, (scalar) -> {
						int row = scalar.getRow();
						int column = scalar.getColumn();
						float value = 0F;
						for (VectorScalar term : confindenceVector) {
							int itemIndex = term.getIndex();
							value += itemFactors.getValue(itemIndex, row) * term.getValue() * itemFactors.getValue(itemIndex, column);
						}
						value += itemSymmetryMatrix.getValue(row, column);
						value += userRegularization;
						scalar.setValue(value);
					});
					// (YtCuY + lambda * itemIdx)^-1
					// lambda * itemIdx can be pre-difined because every time is
					// the
					// same.
					// Yt * (Cu - itemIdx) * Pu + Yt * Pu
					DenseVector userFactorVector = DenseVector.valueOf(numberOfFactors);
					SparseVector preferenceVector = preferenceMatrix.getRowVector(userIndex);
					for (int position = 0, size = preferenceVector.getElementSize(); position < size; position++) {
						int itemIndex = preferenceVector.getIndex(position);
						float confindence = confindenceVector.getValue(position);
						float preference = preferenceVector.getValue(position);
						for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
							userFactorVector.shiftValue(factorIndex, preference * (itemFactors.getValue(itemIndex, factorIndex) * confindence + itemFactors.getValue(itemIndex, factorIndex)));
						}
					}
					// udpate user factors
					userFactors.getRowVector(userIndex).dotProduct(MatrixUtility.inverse(factorMatrix, copyMatrix, inverseMatrix), false, userFactorVector, MathCalculator.SERIAL);
					userLatch.countDown();
				});
			}
			try {
				userLatch.await();
			} catch (Exception exception) {
				throw new RecommendationException(exception);
			}

			// Step 2: update item factors;
			// 按照物品切割任务实现并发计算.
			DenseMatrix userSymmetryMatrix = transposeMatrix;
			userSymmetryMatrix.dotProduct(userFactors, true, userFactors, false, MathCalculator.SERIAL);
			CountDownLatch itemLatch = new CountDownLatch(numberOfItems);
			for (int index = 0; index < numberOfItems; index++) {
				int itemIndex = index;
				context.doAlgorithmByAny(index, () -> {
					DenseMatrix factorMatrix = factorMatrixStorage.get();
					DenseMatrix copyMatrix = copyMatrixStorage.get();
					DenseMatrix inverseMatrix = inverseMatrixStorage.get();
					SparseVector confindenceVector = confindenceMatrix.getColumnVector(itemIndex);
					// XtX + Xt * (Ci - itemIdx) * X
					factorMatrix.iterateElement(MathCalculator.SERIAL, (scalar) -> {
						int row = scalar.getRow();
						int column = scalar.getColumn();
						float value = 0F;
						for (VectorScalar term : confindenceVector) {
							int userIndex = term.getIndex();
							value += userFactors.getValue(userIndex, row) * term.getValue() * userFactors.getValue(userIndex, column);
						}
						value += userSymmetryMatrix.getValue(row, column);
						value += itemRegularization;
						scalar.setValue(value);
					});
					// (XtCuX + lambda * itemIdx)^-1
					// lambda * itemIdx can be pre-difined because every time is
					// the
					// same.
					// Xt * (Ci - itemIdx) * Pu + Xt * Pu
					DenseVector itemFactorVector = DenseVector.valueOf(numberOfFactors);
					SparseVector preferenceVector = preferenceMatrix.getColumnVector(itemIndex);
					for (int position = 0, size = preferenceVector.getElementSize(); position < size; position++) {
						int userIndex = preferenceVector.getIndex(position);
						float confindence = confindenceVector.getValue(position);
						float preference = preferenceVector.getValue(position);
						for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
							itemFactorVector.shiftValue(factorIndex, preference * (userFactors.getValue(userIndex, factorIndex) * confindence + userFactors.getValue(userIndex, factorIndex)));
						}
					}
					// udpate item factors
					itemFactors.getRowVector(itemIndex).dotProduct(MatrixUtility.inverse(factorMatrix, copyMatrix, inverseMatrix), false, itemFactorVector, MathCalculator.SERIAL);
					itemLatch.countDown();
				});
			}
			try {
				itemLatch.await();
			} catch (Exception exception) {
				throw new RecommendationException(exception);
			}

			if (logger.isInfoEnabled()) {
				logger.info(getClass() + " runs at iteration = " + iterationStep + " " + new Date());
			}
		}
	}

}
