package com.jstarcraft.recommendation.utility;

import java.util.concurrent.atomic.AtomicInteger;

import com.jstarcraft.ai.math.algorithm.decomposition.SingularValueDecomposition;
import com.jstarcraft.ai.math.algorithm.distribution.ContinuousProbability;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.MathScalar;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MathMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.MathVector;

/**
 * 矩阵工具
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class MatrixUtility {

	/**
	 * Cholesky矩阵
	 * 
	 * <pre>
	 * http://www.qiujiawei.com/linear-algebra-11/
	 * </pre>
	 * 
	 * @param matrix
	 * @param cholesky
	 * @return
	 */
	// 行列相同,对角一半为0,一半为数值
	// TODO 准备迁移到com.jstarcraft.module.math.algorithm.decomposition
	public static <T extends MathMatrix> T cholesky(final MathMatrix matrix, T cholesky) {
		assert matrix.getRowSize() == matrix.getColumnSize();
		int size = matrix.getColumnSize();
		assert size == cholesky.getRowSize();
		assert size == cholesky.getColumnSize();

		for (int diagonal = 0; diagonal < size; diagonal++) {
			for (int outer = 0; outer <= diagonal; outer++) {
				float sum = 0F;
				for (int inner = 0; inner < outer; inner++) {
					sum += cholesky.getValue(diagonal, inner) * cholesky.getValue(outer, inner);
				}
				float value = (float) (diagonal == outer ? Math.sqrt(matrix.getValue(diagonal, diagonal) - sum)
						: (matrix.getValue(diagonal, outer) - sum) / cholesky.getValue(outer, outer));
				cholesky.setValue(diagonal, outer, value);
			}
			if (Float.isNaN(cholesky.getValue(diagonal, diagonal))) {
				return null;
			}
		}
		return cholesky;
	}

	/**
	 * 协方差矩阵
	 * 
	 * @param matrix
	 * @param outerMeans
	 * @param innerMeans
	 * @param covariance
	 * @return
	 */
	// 行列相同,是对角
	public static <T extends MathMatrix> T covariance(final MathMatrix matrix, MathVector outerMeans,
			MathVector innerMeans, T covariance) {
		assert matrix.getRowSize() == outerMeans.getElementSize();
		assert matrix.getRowSize() == innerMeans.getElementSize();
		int size = matrix.getColumnSize();
		assert size == covariance.getRowSize();
		assert size == covariance.getColumnSize();

		for (int outer = 0; outer < size; outer++) {
			MathScalar scalar = DefaultScalar.getInstance();
			outerMeans.copyVector(matrix.getColumnVector(outer));
			MathScalar outerSum = DefaultScalar.getInstance();
			outerSum.setValue(0F);
			AtomicInteger outerCount = new AtomicInteger();
			outerMeans.iterateElement(MathCalculator.SERIAL, (element) -> {
				if (element.getValue() != 0F) {
					outerSum.shiftValue(element.getValue());
					outerCount.incrementAndGet();
				}
			});
			float outerMean = outerSum.getValue() / outerCount.get();
			outerMeans.iterateElement(MathCalculator.SERIAL, (element) -> {
				element.setValue(element.getValue() - outerMean);
			});
			scalar.dotProduct(outerMeans, outerMeans);
			covariance.setValue(outer, outer, scalar.getValue() / (outerMeans.getElementSize() - 1));
			for (int inner = outer + 1; inner < size; inner++) {
				innerMeans.copyVector(matrix.getColumnVector(inner));
				MathScalar innerSum = DefaultScalar.getInstance();
				innerSum.setValue(0F);
				AtomicInteger innerCount = new AtomicInteger();
				innerMeans.iterateElement(MathCalculator.SERIAL, (element) -> {
					if (element.getValue() != 0F) {
						innerSum.shiftValue(element.getValue());
						innerCount.incrementAndGet();
					}
				});
				float innerMean = innerSum.getValue() / innerCount.get();
				innerMeans.iterateElement(MathCalculator.SERIAL, (element) -> {
					element.setValue(element.getValue() - innerMean);
				});
				scalar.dotProduct(outerMeans, innerMeans);
				float value = scalar.getValue() / (outerMeans.getElementSize() - 1);
				covariance.setValue(outer, inner, value);
				covariance.setValue(inner, outer, value);
			}
		}
		return covariance;
	}

	/**
	 * 逆矩阵
	 * 
	 * @param matrix
	 * @param inverse
	 * @return
	 */
	// 行列相同,非对角
	public static <T extends MathMatrix> T inverse(final MathMatrix matrix, MathMatrix copy, T inverse) {
		assert matrix.getRowSize() == matrix.getColumnSize();
		int size = matrix.getColumnSize();
		assert size == inverse.getRowSize();
		assert size == inverse.getColumnSize();

		inverse.setValues(0F);
		for (int index = 0, length = inverse.getRowSize(); index < length; index++) {
			inverse.setValue(index, index, 1F);
		}
		if (size == 1) {
			inverse.setValue(0, 0, 1 / matrix.getValue(0, 0));
			return inverse;
		}
		copy.copyMatrix(matrix, false);
		for (int index = 0; index < size; index++) {
			// find pivot:
			float mag = 0;
			int pivot = -1;

			for (int row = index; row < size; row++) {
				float value = Math.abs(copy.getValue(row, index));
				if (value > mag) {
					mag = value;
					pivot = row;
				}
			}

			// no pivot (error):
			if (pivot == -1 || mag == 0) {
				return inverse;
			}

			// move pivot row into position:
			if (pivot != index) {
				float value;
				for (int column = 0; column < size; column++) {
					value = inverse.getValue(index, column);
					inverse.setValue(index, column, inverse.getValue(pivot, column));
					inverse.setValue(pivot, column, value);
					if (column >= index) {
						value = copy.getValue(index, column);
						copy.setValue(index, column, copy.getValue(pivot, column));
						copy.setValue(pivot, column, value);
					}
				}
			}

			// normalize pivot row:
			mag = copy.getValue(index, index);

			for (int column = 0; column < size; column++) {
				inverse.setValue(index, column, inverse.getValue(index, column) / mag);
				if (column >= index) {
					copy.setValue(index, column, copy.getValue(index, column) / mag);
				}
			}

			// eliminate pivot row component from other rows:
			for (int row = 0; row < size; row++) {
				if (row == index) {
					continue;
				}
				mag = copy.getValue(row, index);
				for (int column = 0; column < size; column++) {
					if (column >= index) {
						copy.setValue(row, column, copy.getValue(row, column) - mag * copy.getValue(index, column));
					}
					inverse.setValue(row, column,
							inverse.getValue(row, column) - mag * inverse.getValue(index, column));
				}
			}
		}
		return inverse;
	}

	/**
	 * 伪逆矩阵
	 * 
	 * <pre>
	 * http://www.qiujiawei.com/linear-algebra-16/
	 * </pre>
	 * 
	 * @param matrix
	 * @param singular
	 * @param pseudoinverse
	 * @return
	 */
	// 行列可能不相同也可能相同,非对角
	public static <T extends MathMatrix> T pseudoinverse(final MathMatrix matrix, MathMatrix singular,
			T pseudoinverse) {
		assert matrix.getRowSize() == pseudoinverse.getColumnSize();
		assert matrix.getColumnSize() == pseudoinverse.getRowSize();

		SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
		DenseVector singularVector = svd.getS();
		DenseMatrix U = svd.getU(), V = svd.getV();
		// TODO 修改为基于稀疏/哈希矩阵
		DenseMatrix S = DenseMatrix.valueOf(singularVector.getElementSize(), singularVector.getElementSize());
		for (int index = 0; index < singularVector.getElementSize(); index++) {
			float value = singularVector.getValue(index);
			S.setValue(index, index, value == 0F ? 0F : (1F / value));
		}

		singular.dotProduct(V, false, S, false, MathCalculator.SERIAL);
		pseudoinverse.dotProduct(singular, false, U, true, MathCalculator.SERIAL);
		return pseudoinverse;
	}

	/**
	 * 威希矩阵
	 * 
	 * @param matrix
	 * @param normalDistribution
	 * @param gammaDistributions
	 * @param randoms
	 * @param cholesky
	 * @param gaussian
	 * @param gamma
	 * @param transpose
	 * @param wishart
	 * @return
	 */
	// 行列相同,非对称矩阵
	public static <T extends MathMatrix> T wishart(final MathMatrix matrix,
			final ContinuousProbability normalDistribution, final ContinuousProbability[] gammaDistributions,
			MathVector randoms, MathMatrix cholesky, MathMatrix gaussian, MathMatrix gamma, MathMatrix transpose,
			T wishart) {
		cholesky = cholesky(matrix, cholesky);
		if (cholesky == null) {
			// 考虑改为抛异常.
			return null;
		}
		transpose.copyMatrix(cholesky, true);
		cholesky = transpose;
		int size = cholesky.getRowSize();
		// TODO 准备重构,normalDistribution和gammaDistribution改为函数参数.
		gaussian.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(normalDistribution.sample().floatValue());
		});
		randoms.iterateElement(MathCalculator.PARALLEL, (scalar) -> {
			scalar.setValue(gammaDistributions[scalar.getIndex()].sample().floatValue());
		});
		gamma.setValue(0, 0, randoms.getValue(0));

		if (size > 1) {
			// rest of diagonal:
			for (int diagonal = 1; diagonal < size; diagonal++) {
				float value = 0F;
				for (int index = 0; index < diagonal; index++) {
					value += gaussian.getValue(index, diagonal) * gaussian.getValue(index, diagonal);
				}
				gamma.setValue(diagonal, diagonal, randoms.getValue(diagonal) + value);
			}

			// first row and column:
			for (int diagonal = 1; diagonal < size; diagonal++) {
				gamma.setValue(0, diagonal, (float) (gaussian.getValue(0, diagonal) * Math.sqrt(randoms.getValue(0))));
				gamma.setValue(diagonal, 0, gamma.getValue(0, diagonal)); // mirror
			}
		}

		if (size > 2) {
			for (int diagonal = 2; diagonal < size; diagonal++) {
				for (int outer = 1; outer <= diagonal - 1; outer++) {
					float value = 0F;
					for (int inner = 0; inner <= outer - 1; inner++) {
						value += gaussian.getValue(inner, outer) * gaussian.getValue(inner, diagonal);
					}
					gamma.setValue(outer, diagonal,
							(float) (gaussian.getValue(outer, diagonal) * Math.sqrt(randoms.getValue(outer)) + value));
					gamma.setValue(diagonal, outer, gamma.getValue(outer, diagonal)); // mirror
				}
			}
		}
		gaussian.dotProduct(cholesky, true, cholesky, false, MathCalculator.SERIAL);
		wishart.dotProduct(gaussian, false, gamma, false, MathCalculator.SERIAL);
		return wishart;
	}

	/**
	 * 哈达玛矩阵
	 * 
	 * @param left
	 * @param right
	 * @param hadamard
	 * @return
	 */
	public static <T extends MathMatrix> T hadamard(final MathMatrix left, final MathMatrix right, T hadamard) {
		assert left.getRowSize() == right.getRowSize();
		assert left.getColumnSize() == right.getColumnSize();
		assert hadamard.getRowSize() == left.getRowSize();
		assert hadamard.getColumnSize() == right.getColumnSize();

		for (int row = 0; row < left.getRowSize(); row++) {
			for (int column = 0; column < right.getColumnSize(); column++) {
				hadamard.setValue(row, column, left.getValue(row, column) * right.getValue(row, column));
			}
		}
		return hadamard;
	}

	/**
	 * Khatri-Rao矩阵
	 * 
	 * @param left
	 * @param right
	 * @param khatriRao
	 * @return
	 */
	public static <T extends MathMatrix> T khatriRao(final MathMatrix left, final MathMatrix right, T khatriRao) {
		assert khatriRao.getRowSize() == left.getRowSize() * right.getRowSize();
		assert khatriRao.getColumnSize() == left.getColumnSize();
		assert khatriRao.getColumnSize() == right.getColumnSize();

		for (int leftColumn = 0; leftColumn < left.getColumnSize(); leftColumn++) {
			for (int leftRow = 0; leftRow < left.getRowSize(); leftRow++) {
				float leftValue = left.getValue(leftRow, leftColumn);
				for (int rightRow = 0; rightRow < right.getRowSize(); rightRow++) {
					int row = rightRow + leftRow * right.getRowSize();
					float rightValue = right.getValue(rightRow, leftColumn);
					khatriRao.setValue(row, leftColumn, leftValue * rightValue);
				}
			}
		}
		return khatriRao;
	}

	/**
	 * 克罗内克矩阵
	 * 
	 * @param left
	 * @param right
	 * @param kronecker
	 * @return
	 */
	public static <T extends MathMatrix> T kronecker(final MathMatrix left, final MathMatrix right, T kronecker) {
		assert kronecker.getRowSize() == left.getRowSize() * right.getRowSize();
		assert kronecker.getColumnSize() == left.getColumnSize() * right.getColumnSize();
		for (int leftRow = 0; leftRow < left.getRowSize(); leftRow++) {
			for (int leftColumn = 0; leftColumn < left.getColumnSize(); leftColumn++) {
				float leftValue = left.getValue(leftRow, leftColumn);
				for (int rightRow = 0; rightRow < right.getRowSize(); rightRow++) {
					for (int rightColumn = 0; rightColumn < right.getColumnSize(); rightColumn++) {
						float rightValue = right.getValue(rightRow, rightColumn);
						int row = leftRow * right.getRowSize() + rightRow;
						int column = leftColumn * right.getColumnSize() + rightColumn;
						kronecker.setValue(row, column, leftValue * rightValue);
					}
				}
			}
		}
		return kronecker;
	}

}
