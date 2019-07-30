package com.jstarcraft.rns.utility;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.Well19937c;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.nd4j.linalg.factory.Nd4j;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.jstarcraft.ai.environment.EnvironmentContext;
import com.jstarcraft.ai.environment.EnvironmentFactory;
import com.jstarcraft.ai.math.MathUtility;
import com.jstarcraft.ai.math.algorithm.decomposition.SingularValueDecomposition;
import com.jstarcraft.ai.math.algorithm.probability.QuantityProbability;
import com.jstarcraft.ai.math.structure.MathAccessor;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.matrix.SymmetryMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.utility.MatrixUtility;

/**
 * 矩阵测试
 * 
 * @author Birdy
 *
 */
public class MatrixUtilityTestCase {

    private QuantityProbability probability = new QuantityProbability(Well19937c.class, 0, NormalDistribution.class, 1D, 4D);

    @Test
    public void test() throws Exception {
        EnvironmentContext context = EnvironmentFactory.getContext();
        Future<?> task = context.doTask(() -> {
            DenseMatrix matrix = DenseMatrix.valueOf(4, 4);

            matrix.setValues(0F);
            matrix.getRowVector(1).iterateElement(MathCalculator.SERIAL, (scalar) -> {
                scalar.setValue(probability.sample().floatValue());
            });
            System.out.println(matrix);
            System.out.println(DenseMatrix.valueOf(4, 4).copyMatrix(matrix, true));
            System.out.println(DenseMatrix.valueOf(4, 4).dotProduct(matrix, true, matrix, false, MathCalculator.SERIAL));

            matrix.setValues(0F);
            matrix.getColumnVector(1).iterateElement(MathCalculator.SERIAL, (scalar) -> {
                scalar.setValue(probability.sample().floatValue());
            });
            System.out.println(matrix);
            System.out.println(DenseMatrix.valueOf(4, 4).copyMatrix(matrix, true));
            System.out.println(DenseMatrix.valueOf(4, 4).dotProduct(matrix, true, matrix, false, MathCalculator.SERIAL));

            matrix.iterateElement(MathCalculator.PARALLEL, (scalar) -> {
                if (scalar.getRow() == scalar.getColumn()) {
                    scalar.setValue(probability.sample().floatValue());
                } else {
                    scalar.setValue(0F);
                }
            });
            System.out.println(matrix);
            System.out.println(DenseMatrix.valueOf(4, 4).copyMatrix(matrix, true));
            System.out.println(DenseMatrix.valueOf(4, 4).dotProduct(matrix, true, matrix, false, MathCalculator.SERIAL));
        });
        task.get();
    }

    @Test
    public void testSparseMatrix() throws Exception {
        EnvironmentContext context = EnvironmentFactory.getContext();
        Future<?> task = context.doTask(() -> {
            Table<Integer, Integer, Float> dataTable = HashBasedTable.create();
            dataTable.put(0, 0, 1F);
            dataTable.put(0, 1, 2F);
            dataTable.put(0, 3, 3F);
            dataTable.put(1, 0, 4F);
            dataTable.put(2, 0, 5F);
            dataTable.put(2, 1, 6F);
            dataTable.put(3, 1, 7F);
            dataTable.put(3, 3, 8F);

            Float sum = 0F;
            int count = 0;
            SparseMatrix matrix = SparseMatrix.valueOf(4, 4, dataTable);
            for (MatrixScalar term : matrix) {
                count++;
                int row = term.getRow();
                int column = term.getColumn();
                sum += term.getValue();
                Assert.assertThat(term.getValue(), CoreMatchers.equalTo(dataTable.get(row, column)));
            }
            Assert.assertThat(matrix.getElementSize(), CoreMatchers.equalTo(count));
            Assert.assertThat(matrix.getSum(false), CoreMatchers.equalTo(sum));

            for (int row = 0; row < 4; row++) {
                SparseVector vector = matrix.getRowVector(row);
                sum = 0F;
                count = 0;
                for (VectorScalar term : vector) {
                    count++;
                    sum += term.getValue();
                }
                Assert.assertThat(vector.getElementSize(), CoreMatchers.equalTo(count));
                Assert.assertThat(vector.getSum(false), CoreMatchers.equalTo(sum));
            }

            for (int column = 0; column < 4; column++) {
                SparseVector vector = matrix.getColumnVector(column);
                sum = 0F;
                count = 0;
                for (VectorScalar term : vector) {
                    count++;
                    sum += term.getValue();
                }
                Assert.assertThat(vector.getElementSize(), CoreMatchers.equalTo(count));
                Assert.assertThat(vector.getSum(false), CoreMatchers.equalTo(sum));
            }

            sum = 0F;
            count = 0;
            matrix = SparseMatrix.copyOf(matrix, true);
            for (MatrixScalar term : matrix) {
                count++;
                int row = term.getRow();
                int column = term.getColumn();
                sum += term.getValue();
                Assert.assertThat(term.getValue(), CoreMatchers.equalTo(dataTable.get(column, row)));
            }
            Assert.assertThat(matrix.getElementSize(), CoreMatchers.equalTo(count));
            Assert.assertThat(matrix.getSum(false), CoreMatchers.equalTo(sum));
        });
        task.get();
    }

    /**
     * 测试矩阵乘法
     */
    @Test
    public void testDotProduct() throws Exception {
        EnvironmentContext context = EnvironmentFactory.getContext();
        Future<?> task = context.doTask(() -> {
            DenseMatrix matrix = DenseMatrix.valueOf(3, 4);
            matrix.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                scalar.setValue(probability.sample().floatValue());
            });

            System.out.println("print matrix Matrix:");
            System.out.println(matrix.toString());

            Table<Integer, Integer, Float> dataTable = HashBasedTable.create();
            dataTable.put(0, 0, RandomUtility.randomFloat(1F));
            dataTable.put(0, 1, RandomUtility.randomFloat(1F));
            dataTable.put(0, 3, RandomUtility.randomFloat(1F));
            dataTable.put(1, 0, RandomUtility.randomFloat(1F));
            dataTable.put(2, 0, RandomUtility.randomFloat(1F));
            dataTable.put(2, 1, RandomUtility.randomFloat(1F));
            dataTable.put(3, 1, RandomUtility.randomFloat(1F));
            dataTable.put(3, 3, RandomUtility.randomFloat(1F));
            // 稀疏矩阵
            SparseMatrix sparse = SparseMatrix.valueOf(4, 4, dataTable);
            // 稠密矩阵
            DenseMatrix dense = DenseMatrix.valueOf(4, 4);
            for (MatrixScalar entry : sparse) {
                int row = entry.getRow();
                int column = entry.getColumn();
                float value = entry.getValue();
                dense.setValue(row, column, value);
            }
            DenseMatrix left = DenseMatrix.valueOf(matrix.getRowSize(), dense.getColumnSize());
            DenseMatrix right = DenseMatrix.valueOf(matrix.getRowSize(), dense.getColumnSize());
            Assert.assertThat(left.dotProduct(matrix, false, sparse, false, MathCalculator.PARALLEL), CoreMatchers.equalTo(right.dotProduct(matrix, false, dense, false, MathCalculator.PARALLEL)));
            left.dotProduct(matrix, false, sparse, false, MathCalculator.PARALLEL);
            Assert.assertThat(left, CoreMatchers.equalTo(right));
            right.dotProduct(matrix, false, dense, false, MathCalculator.PARALLEL);
            Assert.assertThat(right, CoreMatchers.equalTo(left));
            // 归一化
            sparse.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                scalar.setValue((scalar.getValue() - -10F) / (10F - -10F));
            });
            for (MatrixScalar entry : sparse) {
                int row = entry.getRow();
                int column = entry.getColumn();
                float value = entry.getValue();
                dense.setValue(row, column, value);
            }
            Assert.assertThat(left.dotProduct(matrix, false, sparse, false, MathCalculator.PARALLEL), CoreMatchers.equalTo(right.dotProduct(matrix, false, dense, false, MathCalculator.PARALLEL)));

            System.out.println("print matrix Dense:");
            System.out.println(dense);
            System.out.println("print matrix Sparse:");
            System.out.println(sparse);
            System.out.println("print matrix Matrix*Sparse:");
            System.out.println(left.dotProduct(matrix, false, sparse, false, MathCalculator.PARALLEL));
            System.out.println("print matrix Matrix*Dense:");
            System.out.println(right.dotProduct(matrix, false, dense, false, MathCalculator.PARALLEL));

            // 稀疏矩阵
            DenseMatrix transpose = DenseMatrix.valueOf(matrix.getColumnSize(), matrix.getRowSize());
            transpose.copyMatrix(matrix, true);
            left = DenseMatrix.valueOf(sparse.getRowSize(), transpose.getColumnSize());
            right = DenseMatrix.valueOf(sparse.getRowSize(), transpose.getColumnSize());
            Assert.assertThat(left.dotProduct(sparse, false, transpose, false, MathCalculator.SERIAL), CoreMatchers.equalTo(right.dotProduct(dense, false, transpose, false, MathCalculator.SERIAL)));
        });
        task.get();
    }

    /**
     * 测试矩阵转置乘法
     */
    @Test
    public void testTransposeProduct() throws Exception {
        EnvironmentContext context = EnvironmentFactory.getContext();
        Future<?> task = context.doTask(() -> {
            DenseMatrix matrix = DenseMatrix.valueOf(4, 3);
            matrix.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                scalar.setValue(probability.sample().floatValue());
            });

            System.out.println("print matrix Matrix:");
            System.out.println(matrix.toString());

            Table<Integer, Integer, Float> dataTable = HashBasedTable.create();
            dataTable.put(0, 0, RandomUtility.randomFloat(1F));
            dataTable.put(0, 1, RandomUtility.randomFloat(1F));
            dataTable.put(0, 3, RandomUtility.randomFloat(1F));
            dataTable.put(1, 0, RandomUtility.randomFloat(1F));
            dataTable.put(2, 0, RandomUtility.randomFloat(1F));
            dataTable.put(2, 1, RandomUtility.randomFloat(1F));
            dataTable.put(3, 1, RandomUtility.randomFloat(1F));
            dataTable.put(3, 3, RandomUtility.randomFloat(1F));
            // 稀疏矩阵
            SparseMatrix sparse = SparseMatrix.valueOf(4, 4, dataTable);
            // 稠密矩阵
            DenseMatrix dense = DenseMatrix.valueOf(4, 4);
            for (MatrixScalar term : sparse) {
                int row = term.getRow();
                int column = term.getColumn();
                float value = term.getValue();
                dense.setValue(row, column, value);
            }
            DenseMatrix transpose = DenseMatrix.valueOf(matrix.getColumnSize(), dense.getRowSize());
            DenseMatrix left = DenseMatrix.valueOf(matrix.getColumnSize(), dense.getColumnSize());
            DenseMatrix right = DenseMatrix.valueOf(matrix.getColumnSize(), dense.getColumnSize());
            Assert.assertThat(left.dotProduct(matrix, true, dense, false, MathCalculator.SERIAL), CoreMatchers.equalTo(right.dotProduct(transpose.copyMatrix(matrix, true), false, dense, false, MathCalculator.SERIAL)));
            Assert.assertThat(left.dotProduct(matrix, true, sparse, false, MathCalculator.SERIAL), CoreMatchers.equalTo(right.dotProduct(transpose.copyMatrix(matrix, true), false, dense, false, MathCalculator.SERIAL)));
            left.dotProduct(matrix, true, sparse, false, MathCalculator.SERIAL);
            Assert.assertThat(left, CoreMatchers.equalTo(right));
            right.dotProduct(matrix, true, dense, false, MathCalculator.SERIAL);
            Assert.assertThat(right, CoreMatchers.equalTo(left));
        });
        task.get();
    }

    /**
     * 测试伪逆矩阵
     */
    @Test
    public void testPseudoinverse() throws Exception {
        EnvironmentContext context = EnvironmentFactory.getContext();
        Future<?> task = context.doTask(() -> {
            // 伪逆矩阵
            DenseMatrix matrix = DenseMatrix.valueOf(4, 3);
            matrix.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                scalar.setValue(probability.sample().floatValue());
            });
            System.out.println("print matrix Matrix:");
            System.out.println(matrix.toString());
            DenseMatrix singular = DenseMatrix.valueOf(matrix.getColumnSize(), matrix.getColumnSize());
            DenseMatrix pseudoinverse = DenseMatrix.valueOf(matrix.getColumnSize(), matrix.getRowSize());
            MatrixUtility.pseudoinverse(matrix, singular, pseudoinverse);
            System.out.println("print pseudoinverse Matrix:");
            System.out.println(pseudoinverse.toString());

            Assert.assertThat(pseudoinverse.getRowSize(), CoreMatchers.equalTo(matrix.getColumnSize()));
            Assert.assertThat(pseudoinverse.getColumnSize(), CoreMatchers.equalTo(matrix.getRowSize()));

            // 伪逆矩阵的目标是AXA=A,XAX=X.且X与A.transpose行列相同.
            // 由于精度问题,所以使用transformer确定是否为0.
            MathAccessor<MatrixScalar> accessor = (scalar) -> {
                if (scalar.getValue() >= MathUtility.EPSILON) {
                    Assert.fail();
                }
            };
            DenseMatrix left;
            DenseMatrix right;

            left = DenseMatrix.valueOf(matrix.getRowSize(), pseudoinverse.getColumnSize());
            right = DenseMatrix.valueOf(matrix.getRowSize(), matrix.getColumnSize());
            left.dotProduct(matrix, false, pseudoinverse, false, MathCalculator.SERIAL);
            right.dotProduct(left, false, matrix, false, MathCalculator.SERIAL);
            System.out.println(right);
            right.subtractMatrix(matrix, false).iterateElement(MathCalculator.PARALLEL, accessor);

            left = DenseMatrix.valueOf(pseudoinverse.getRowSize(), matrix.getColumnSize());
            right = DenseMatrix.valueOf(pseudoinverse.getRowSize(), pseudoinverse.getColumnSize());
            left.dotProduct(pseudoinverse, false, matrix, false, MathCalculator.SERIAL);
            right.dotProduct(left, false, pseudoinverse, false, MathCalculator.SERIAL);
            System.out.println(right);
            right.subtractMatrix(pseudoinverse, false).iterateElement(MathCalculator.PARALLEL, accessor);
        });
        task.get();
    }

    /**
     * 测试矩阵奇异值分解
     */
    @Test
    public void testSVD() throws Exception {
        EnvironmentContext context = EnvironmentFactory.getContext();
        Future<?> task = context.doTask(() -> {
            // 注意:矩阵必须row大于等于column
            int rowSize = 5;
            int columnSize = 3;
            AtomicInteger random = new AtomicInteger();
            DenseMatrix oldMatrix = DenseMatrix.valueOf(rowSize, columnSize);
            oldMatrix.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                scalar.setValue(random.getAndIncrement());
            });

            SingularValueDecomposition svd = new SingularValueDecomposition(oldMatrix);
            System.out.println("print matrix U:");
            System.out.println(svd.getU().toString());

            System.out.println("print vector S:");
            System.out.println(svd.getS().toString());

            System.out.println("print matrix V:");
            System.out.println(svd.getV().toString());

            DenseVector singularVector = svd.getS();

            // TODO 修改为基于稀疏/哈希矩阵
            DenseMatrix S = DenseMatrix.valueOf(singularVector.getElementSize(), singularVector.getElementSize());
            for (int index = 0; index < singularVector.getElementSize(); index++) {
                S.setValue(index, index, singularVector.getValue(index));
            }

            DenseMatrix middleMatrix = DenseMatrix.valueOf(rowSize, columnSize);
            DenseMatrix transpose = DenseMatrix.valueOf(svd.getV().getColumnSize(), svd.getV().getRowSize());
            DenseMatrix newMatrix = DenseMatrix.valueOf(rowSize, columnSize);
            middleMatrix.dotProduct(svd.getU(), false, S, false, MathCalculator.SERIAL);
            newMatrix.dotProduct(middleMatrix, false, transpose.copyMatrix(svd.getV(), true), false, MathCalculator.SERIAL);

            System.out.println("print matrix OLD:");
            System.out.println(oldMatrix.toString());
            System.out.println("print matrix NEW:");
            System.out.println(newMatrix.toString());

            // 由于计算导致精度的损失,所以此处不直接使用DenseMatrix.equals比较.
            for (int row = 0; row < rowSize; row++) {
                for (int column = 0; column < columnSize; column++) {
                    float oldValue = oldMatrix.getValue(row, column);
                    float newValue = newMatrix.getValue(row, column);
                    if (!MathUtility.equal(oldValue, newValue)) {
                        System.out.println(oldValue);
                        System.out.println(newValue);
                        Assert.fail();
                    }
                }
            }
        });
        task.get();
    }

    @Test
    public void testSymmetryMatrix() throws Exception {
        EnvironmentContext context = EnvironmentFactory.getContext();
        Future<?> task = context.doTask(() -> {
            SymmetryMatrix matrix = new SymmetryMatrix(5);
            Assert.assertThat(matrix.getElementSize(), CoreMatchers.equalTo(15));
            int times = 0;
            for (MatrixScalar term : matrix) {
                times++;
                term.setValue(times);
                Assert.assertTrue(term.getValue() == times);
                Assert.assertTrue(matrix.getValue(term.getRow(), term.getColumn()) == times);
            }
            Assert.assertThat(times, CoreMatchers.equalTo(15));

            // 对称矩阵无论转置多少次都不变.
            DenseMatrix symmetry = DenseMatrix.valueOf(3, 3);
            symmetry.setValue(0, 0, 25F);
            symmetry.setValue(0, 1, 15F);
            symmetry.setValue(1, 0, 15F);
            symmetry.setValue(1, 1, 18F);
            symmetry.setValue(0, 2, -5F);
            symmetry.setValue(2, 0, -5F);
            symmetry.setValue(2, 2, 11F);
            DenseMatrix transpose = DenseMatrix.valueOf(symmetry.getColumnSize(), symmetry.getRowSize());
            Assert.assertThat(transpose.copyMatrix(symmetry, true), CoreMatchers.equalTo(symmetry));

            DenseMatrix left = DenseMatrix.valueOf(symmetry.getRowSize(), symmetry.getColumnSize());
            DenseMatrix right = DenseMatrix.valueOf(symmetry.getRowSize(), symmetry.getColumnSize());

            // Cholesky分解:http://www.qiujiawei.com/linear-algebra-11/
            // (Cholesky分解的目标是把A变成:A=LLT,L是下三角矩阵.)
            DenseMatrix cholesky = DenseMatrix.valueOf(3, 3);
            MatrixUtility.cholesky(symmetry, cholesky);
            Assert.assertThat(left.dotProduct(cholesky, false, transpose.copyMatrix(cholesky, true), false, MathCalculator.SERIAL), CoreMatchers.equalTo(symmetry));

            // 协方差矩阵(本质是对称矩阵)
            DenseVector outerMeans = DenseVector.valueOf(3);
            DenseVector innerMeans = DenseVector.valueOf(3);
            DenseMatrix covariance = DenseMatrix.valueOf(3, 3);
            MatrixUtility.covariance(cholesky, outerMeans, innerMeans, covariance);
            Assert.assertThat(transpose.copyMatrix(covariance, true), CoreMatchers.equalTo(covariance));

            // 逆矩阵的目标是AB=BA=E(E是单位矩阵,对角都是1,其它都是0)
            // 由于精度问题,所以使用transformer将矩阵修改为单位矩阵.
            MathAccessor<MatrixScalar> accessor = (scalar) -> {
                int row = scalar.getRow();
                int column = scalar.getColumn();
                float value = scalar.getValue();
                if (row == column) {
                    if (!MathUtility.equal(value, 1F)) {
                        System.err.println(value);
                        Assert.fail();
                    }
                    scalar.setValue(1F);
                } else {
                    if (!MathUtility.equal(value, 0F)) {
                        System.err.println(value);
                        Assert.fail();
                    }
                    scalar.setValue(0F);
                }
            };
            DenseMatrix inverse = DenseMatrix.valueOf(3, 3);
            DenseMatrix copy = DenseMatrix.valueOf(3, 3);
            MatrixUtility.inverse(symmetry, copy, inverse);
            Assert.assertThat(left.dotProduct(symmetry, false, inverse, false, MathCalculator.SERIAL).iterateElement(MathCalculator.PARALLEL, accessor), CoreMatchers.equalTo(right.dotProduct(inverse, false, symmetry, false, MathCalculator.SERIAL).iterateElement(MathCalculator.PARALLEL, accessor)));

            inverse = MatrixUtility.inverse(cholesky, copy, inverse);
            Assert.assertThat(left.dotProduct(cholesky, false, inverse, false, MathCalculator.SERIAL).iterateElement(MathCalculator.PARALLEL, accessor), CoreMatchers.equalTo(right.dotProduct(inverse, false, cholesky, false, MathCalculator.SERIAL).iterateElement(MathCalculator.PARALLEL, accessor)));

            inverse = MatrixUtility.inverse(covariance, copy, inverse);
            Assert.assertThat(left.dotProduct(covariance, false, inverse, false, MathCalculator.SERIAL).iterateElement(MathCalculator.PARALLEL, accessor), CoreMatchers.equalTo(right.dotProduct(inverse, false, covariance, false, MathCalculator.SERIAL).iterateElement(MathCalculator.PARALLEL, accessor)));
        });
        task.get();
    }

}
