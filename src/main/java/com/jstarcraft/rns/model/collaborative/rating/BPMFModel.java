package com.jstarcraft.rns.model.collaborative.rating;

import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.MatrixUtility;
import com.jstarcraft.ai.math.algorithm.probability.QuantityProbability;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.rns.model.MatrixFactorizationModel;
import com.jstarcraft.rns.model.exception.ModelException;

/**
 * 
 * BPMF推荐器
 * 
 * <pre>
 * Bayesian Probabilistic Matrix Factorization using Markov Chain Monte Carlo
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class BPMFModel extends MatrixFactorizationModel {

    private float userMean, userWishart;

    private float itemMean, itemWishart;

    private float userBeta, itemBeta;

    private float rateSigma;

    private int gibbsIterations;

    private DenseMatrix[] userMatrixes;

    private DenseMatrix[] itemMatrixes;

    private QuantityProbability normalDistribution;
    private QuantityProbability[] userGammaDistributions;
    private QuantityProbability[] itemGammaDistributions;

    private class HyperParameter {

        // 缓存
        private float[] thisVectorCache;
        private float[] thatVectorCache;
        private float[] thisMatrixCache;
        private float[] thatMatrixCache;

        private DenseVector factorMeans;

        private DenseMatrix factorVariances;

        private DenseVector randoms;

        private DenseVector outerMeans, innerMeans;

        private DenseMatrix covariance, cholesky, inverse, transpose, gaussian, gamma, wishart, copy;

        HyperParameter(int cache, DenseMatrix factors) {
            if (cache < factorSize) {
                cache = factorSize;
            }
            thisVectorCache = new float[cache];
            thisMatrixCache = new float[cache * factorSize];
            thatVectorCache = new float[cache];
            thatMatrixCache = new float[cache * factorSize];

            factorMeans = DenseVector.valueOf(factorSize);
            float scale = factors.getRowSize();
            for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                factorMeans.setValue(factorIndex, factors.getColumnVector(factorIndex).getSum(false) / scale);
            }
            outerMeans = DenseVector.valueOf(factors.getRowSize());
            innerMeans = DenseVector.valueOf(factors.getRowSize());
            covariance = DenseMatrix.valueOf(factorSize, factorSize);

            cholesky = DenseMatrix.valueOf(factorSize, factorSize);

            inverse = DenseMatrix.valueOf(factorSize, factorSize);
            transpose = DenseMatrix.valueOf(factorSize, factorSize);

            randoms = DenseVector.valueOf(factorSize);
            gaussian = DenseMatrix.valueOf(factorSize, factorSize);
            gamma = DenseMatrix.valueOf(factorSize, factorSize);
            wishart = DenseMatrix.valueOf(factorSize, factorSize);

            copy = DenseMatrix.valueOf(factorSize, factorSize);

            factorVariances = MatrixUtility.inverse(MatrixUtility.covariance(factors, outerMeans, innerMeans, covariance), copy, inverse);
        }

        /**
         * 取样
         * 
         * @param hyperParameter
         * @param factors
         * @param normalMu
         * @param normalBeta
         * @param wishartScale
         * @return
         * @throws ModelException
         */
        private void sampleParameter(QuantityProbability[] gammaDistributions, DenseMatrix factors, float normalMu, float normalBeta, float wishartScale) throws ModelException {
            int rowSize = factors.getRowSize();
            int columnSize = factors.getColumnSize();
            // 重复利用内存.
            DenseVector meanCache = DenseVector.valueOf(factorSize, thisVectorCache);
            float scale = factors.getRowSize();
            meanCache.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                int index = scalar.getIndex();
                float value = scalar.getValue();
                scalar.setValue(factors.getColumnVector(index).getSum(false) / scale);
            });
            float beta = normalBeta + rowSize;
            DenseMatrix populationVariance = MatrixUtility.covariance(factors, outerMeans, innerMeans, covariance);
            wishart.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                int row = scalar.getRow();
                int column = scalar.getColumn();
                float value = 0F;
                if (row == column) {
                    value = wishartScale;
                }
                value += populationVariance.getValue(row, column) * rowSize;
                value += (normalMu - meanCache.getValue(row)) * (normalMu - meanCache.getValue(column)) * (normalBeta * rowSize / beta);
                scalar.setValue(value);
            });
            DenseMatrix wishartMatrix = wishart;
            wishartMatrix = MatrixUtility.inverse(wishartMatrix, copy, inverse);
            wishartMatrix.addMatrix(transpose.copyMatrix(wishartMatrix, true), false).scaleValues(0.5F);
            wishartMatrix = MatrixUtility.wishart(wishartMatrix, normalDistribution, gammaDistributions, randoms, cholesky, gaussian, gamma, transpose, wishart);
            if (wishartMatrix != null) {
                factorVariances = wishartMatrix;
            }
            DenseMatrix normalVariance = MatrixUtility.cholesky(MatrixUtility.inverse(factorVariances, copy, inverse).scaleValues(normalBeta), cholesky);
            if (normalVariance != null) {
                randoms.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                    scalar.setValue(normalDistribution.sample().floatValue());
                });
                factorMeans.dotProduct(normalVariance, false, randoms, MathCalculator.SERIAL).iterateElement(MathCalculator.SERIAL, (scalar) -> {
                    int index = scalar.getIndex();
                    float value = scalar.getValue();
                    scalar.setValue(value + (normalMu * normalBeta + meanCache.getValue(index) * rowSize) * (1F / beta));
                });
            }
        }

        /**
         * 更新
         * 
         * @param factorMatrix
         * @param scoreVector
         * @param hyperParameter
         * @return
         * @throws ModelException
         */
        private void updateParameter(DenseMatrix factorMatrix, SparseVector scoreVector, DenseVector factorVector) throws ModelException {
            int size = scoreVector.getElementSize();
            // 重复利用内存.
            DenseMatrix factorCache = DenseMatrix.valueOf(size, factorSize, thisMatrixCache);
            MathVector meanCache = DenseVector.valueOf(size, thisVectorCache);
            int index = 0;
            for (VectorScalar term : scoreVector) {
                meanCache.setValue(index, term.getValue() - meanScore);
                MathVector vector = factorMatrix.getRowVector(term.getIndex());
                factorCache.getRowVector(index).iterateElement(MathCalculator.SERIAL, (scalar) -> {
                    scalar.setValue(vector.getValue(scalar.getIndex()));
                });
                index++;
            }
            transpose.dotProduct(factorCache, true, factorCache, false, MathCalculator.SERIAL);
            transpose.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                int row = scalar.getRow();
                int column = scalar.getColumn();
                float value = scalar.getValue();
                scalar.setValue(value * rateSigma + factorVariances.getValue(row, column));
            });
            DenseMatrix covariance = transpose;
            covariance = MatrixUtility.inverse(covariance, copy, inverse);
            // 重复利用内存.
            meanCache = DenseVector.valueOf(factorCache.getColumnSize(), thatVectorCache).dotProduct(factorCache, true, meanCache, MathCalculator.SERIAL);
            meanCache.scaleValues(rateSigma);
            // 重复利用内存.
            meanCache.addVector(DenseVector.valueOf(factorVariances.getRowSize(), thisVectorCache).dotProduct(factorVariances, false, factorMeans, MathCalculator.SERIAL));
            // 重复利用内存.
            meanCache = DenseVector.valueOf(covariance.getRowSize(), thisVectorCache).dotProduct(covariance, false, meanCache, MathCalculator.SERIAL);
            covariance = MatrixUtility.cholesky(covariance, cholesky);
            if (covariance != null) {
                randoms.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                    scalar.setValue(normalDistribution.sample().floatValue());
                });
                factorVector.dotProduct(covariance, false, randoms, MathCalculator.SERIAL).addVector(meanCache);
            } else {
                factorVector.setValues(0F);
            }
        }
    }

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        userMean = configuration.getFloat("recommender.recommender.user.mu", 0F);
        userBeta = configuration.getFloat("recommender.recommender.user.beta", 1F);
        userWishart = configuration.getFloat("recommender.recommender.user.wishart.scale", 1F);

        itemMean = configuration.getFloat("recommender.recommender.item.mu", 0F);
        itemBeta = configuration.getFloat("recommender.recommender.item.beta", 1F);
        itemWishart = configuration.getFloat("recommender.recommender.item.wishart.scale", 1F);

        rateSigma = configuration.getFloat("recommender.recommender.rating.sigma", 2F);

        gibbsIterations = configuration.getInteger("recommender.recommender.iterations.gibbs", 1);

        userMatrixes = new DenseMatrix[epocheSize - 1];
        itemMatrixes = new DenseMatrix[epocheSize - 1];

        normalDistribution = new QuantityProbability(JDKRandomGenerator.class, factorSize, NormalDistribution.class, 0D, 1D);
        userGammaDistributions = new QuantityProbability[factorSize];
        itemGammaDistributions = new QuantityProbability[factorSize];
        for (int index = 0; index < factorSize; index++) {
            userGammaDistributions[index] = new QuantityProbability(JDKRandomGenerator.class, index, GammaDistribution.class, (userSize + factorSize - (index + 1D)) / 2D, 2D);
            itemGammaDistributions[index] = new QuantityProbability(JDKRandomGenerator.class, index, GammaDistribution.class, (itemSize + factorSize - (index + 1D)) / 2D, 2D);
        }
    }

    @Override
    protected void doPractice() {
        int cacheSize = 0;
        SparseVector[] userVectors = new SparseVector[userSize];
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            SparseVector userVector = scoreMatrix.getRowVector(userIndex);
            cacheSize = cacheSize < userVector.getElementSize() ? userVector.getElementSize() : cacheSize;
            userVectors[userIndex] = userVector;
        }

        SparseVector[] itemVectors = new SparseVector[itemSize];
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            SparseVector itemVector = scoreMatrix.getColumnVector(itemIndex);
            cacheSize = cacheSize < itemVector.getElementSize() ? itemVector.getElementSize() : cacheSize;
            itemVectors[itemIndex] = itemVector;
        }

        // TODO 此处考虑重构
        HyperParameter userParameter = new HyperParameter(cacheSize, userFactors);
        HyperParameter itemParameter = new HyperParameter(cacheSize, itemFactors);
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            userParameter.sampleParameter(userGammaDistributions, userFactors, userMean, userBeta, userWishart);
            itemParameter.sampleParameter(itemGammaDistributions, itemFactors, itemMean, itemBeta, itemWishart);
            for (int gibbsIteration = 0; gibbsIteration < gibbsIterations; gibbsIteration++) {
                for (int userIndex = 0; userIndex < userSize; userIndex++) {
                    SparseVector scoreVector = userVectors[userIndex];
                    if (scoreVector.getElementSize() == 0) {
                        continue;
                    }
                    userParameter.updateParameter(itemFactors, scoreVector, userFactors.getRowVector(userIndex));
                }
                for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                    SparseVector scoreVector = itemVectors[itemIndex];
                    if (scoreVector.getElementSize() == 0) {
                        continue;
                    }
                    itemParameter.updateParameter(userFactors, scoreVector, itemFactors.getRowVector(itemIndex));
                }
            }

            if (epocheIndex > 0) {
                userMatrixes[epocheIndex - 1] = DenseMatrix.copyOf(userFactors);
                itemMatrixes[epocheIndex - 1] = DenseMatrix.copyOf(itemFactors);
            }
        }
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        DefaultScalar scalar = DefaultScalar.getInstance();
        float value = 0F;
        for (int iterationStep = 0; iterationStep < epocheSize - 1; iterationStep++) {
            DenseVector userVector = userMatrixes[iterationStep].getRowVector(userIndex);
            DenseVector itemVector = itemMatrixes[iterationStep].getRowVector(itemIndex);
            value = (value * (iterationStep) + meanScore + scalar.dotProduct(userVector, itemVector).getValue()) / (iterationStep + 1);
        }
        instance.setQuantityMark(value);
    }

}
