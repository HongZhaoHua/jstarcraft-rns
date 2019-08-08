package com.jstarcraft.rns.recommend.collaborative.ranking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.AtomicDouble;
import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.attribute.MemoryQualityAttribute;
import com.jstarcraft.ai.environment.EnvironmentContext;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.recommend.ProbabilisticGraphicalRecommender;
import com.jstarcraft.rns.recommend.exception.RecommendException;
import com.jstarcraft.rns.utility.GammaUtility;

/**
 * 
 * HMMForCF推荐器
 * 
 * <pre>
 * A Hidden Markov Model Purpose: A class for the model, including parameters
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class HMMRecommender extends ProbabilisticGraphicalRecommender {

    private static float nearZero = (float) Math.pow(10F, -10F);

    /** 上下文字段 */
    private String contextField;

    /** 上下文维度 */
    private int contextDimension;

    /** 状态数 */
    private int numberOfStates;

    /**
     * <pre>
     * 正则化参数
     * probabilityRegularization,stateRegularization必须大于numberOfStates
     * viewRegularization必须大于numberOfItems
     * 否则可能导致Formula 1.9的viewProbabilities元素可能为NaN
     * </pre>
     */
    private float probabilityRegularization, stateRegularization, viewRegularization;

    private DenseVector probabilityNumerator;
    private AtomicDouble probabilityDenominator = new AtomicDouble();
    private DenseMatrix stateNumerator;
    private DenseVector stateDenominator;
    private DenseMatrix viewNumerator;
    private DenseVector viewDenominator;

    /** 概率向量(Pi) {numberOfStates} */
    private DenseVector probabilities;

    /** 状态概率矩阵(A) {numberOfStates, numberOfStates} */
    private DenseMatrix stateProbabilities;

    /** 观察概率矩阵(B) {numberOfStates, numberOfItems} */
    private DenseMatrix viewProbabilities;

    // numeratorPsiGamma => {numberOfStates}
    private DenseVector numeratorPsiGamma;
    // numeratorLogGamma=> {numberOfStates}
    private DenseVector numeratorLogGamma;
    // denominatorPsiGamma => {numberOfStates}
    private DenseVector denominatorPsiGamma;
    // denominatorLogGamma => {numberOfStates}
    private DenseVector denominatorLogGamma;
    // psiNumerator => {numberOfStates}
    private DenseVector psiNumerator;
    // nutNumerator => {numberOfStates}
    private DenseVector nutNumerator;
    // next_denominator => {numberOfStates}
    private DenseVector averageDenominator;
    // numerator => {numberOfStates}
    private DenseVector modelNumerator;
    // denominator => {numberOfStates}
    private DenseVector modelDenominator;

    /** 负二项分布 */
    private DenseVector alpha, beta;

    /**
     * TODO 似乎与用户有关联
     * 
     * <pre>
     * P(Z(u,t)|I(u,1:T)) |numberOfUsers|*(|sizeOfContexts|*|numberOfStates|)
     * </pre>
     */
    private DenseMatrix[] gammas;

    /**
     * TODO 似乎与上下文有关联
     * 
     * <pre>
     * P(Z(u,t-1),Z(u,t)|I(u,1:T)) |numberOfUsers|*(|numberOfStates|*|numberOfStates|)
     * </pre>
     */

    /**
     * TODO 与总和有关
     * 
     * <pre>
     * |numberOfUsers|*(|sizeOfContexts|)
     * </pre>
     */
    private DenseVector[] nuts;

    private DenseMatrix norms;

    /** 数据矩阵集合 {sizeOfContexts, numberOfItems} */
    private SparseMatrix[] dataMatrixes;

    /** 上下文大小(缓存相关) */
    private int contextSize;

    /**
     * 检查模型
     * 
     * @param vector
     * @return
     */
    private boolean checkVector(DenseVector vector) {
        for (VectorScalar term : vector) {
            if (Float.isNaN(term.getValue())) {
                return false;
            }
            if (Float.isInfinite(term.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查模型
     * 
     * @param matrix
     * @return
     */
    private boolean checkMatrix(DenseMatrix matrix) {
        for (MatrixScalar term : matrix) {
            if (Float.isNaN(term.getValue())) {
                return false;
            }
            if (Float.isInfinite(term.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查模型
     * 
     * @param model
     * @return
     */
    private boolean checkModel(DenseVector model) {
        for (VectorScalar term : model) {
            if (Float.isNaN(term.getValue())) {
                return false;
            }
            if (Float.isInfinite(term.getValue())) {
                return false;
            }
            // psiGamma遇到负整数会变为NaN或者无穷.
            // logGamma遇到负数会变为NaN或者无穷.
            if (term.getValue() <= 0F) {
                return false;
            }
        }
        return true;
    }

    // 线程缓存
    // E Step
    private ThreadLocal<float[]> alphaStorage = new ThreadLocal<>();
    private ThreadLocal<float[]> betaStorage = new ThreadLocal<>();
    private ThreadLocal<float[]> rhoStorage = new ThreadLocal<>();
    private ThreadLocal<float[]> binomialStorage = new ThreadLocal<>();
    private ThreadLocal<float[]> multinomialStorage = new ThreadLocal<>();
    private ThreadLocal<float[]> normContextStorage = new ThreadLocal<>();
    private ThreadLocal<float[]> normStateStorage = new ThreadLocal<>();
    private ThreadLocal<float[]> sumGammaStorage = new ThreadLocal<>();
    private ThreadLocal<float[]> gammaSumStorage = new ThreadLocal<>();

    // M Step
    private ThreadLocal<DenseVector> numeratorStorage = new ThreadLocal<>();
    private ThreadLocal<DenseVector> denominatorStorage = new ThreadLocal<>();
    private ThreadLocal<DenseVector> probabilityNumeratorStorage = new ThreadLocal<>();
    private ThreadLocal<AtomicDouble> probabilityDenominatorStorage = new ThreadLocal<>();
    private ThreadLocal<DenseMatrix> stateNumeratorStorage = new ThreadLocal<>();
    private ThreadLocal<DenseMatrix> viewNumeratorStorage = new ThreadLocal<>();
    private ThreadLocal<DenseVector> viewDenominatorStorage = new ThreadLocal<>();

    @Override
    protected void constructEnvironment() {
        // E Step并发计算部分
        alphaStorage.set(new float[contextSize * numberOfStates]);
        betaStorage.set(new float[contextSize * numberOfStates]);
        rhoStorage.set(new float[numberOfStates * numberOfStates]);
        normContextStorage.set(new float[contextSize]);
        normStateStorage.set(new float[numberOfStates]);
        sumGammaStorage.set(new float[contextSize]);
        gammaSumStorage.set(new float[contextSize]);
        binomialStorage.set(new float[contextSize * numberOfStates]);
        multinomialStorage.set(new float[contextSize * numberOfStates]);

        // M Step并发计算部分
        numeratorStorage.set(DenseVector.valueOf(numberOfStates));
        denominatorStorage.set(DenseVector.valueOf(numberOfStates));

        probabilityNumeratorStorage.set(DenseVector.valueOf(numberOfStates));
        probabilityDenominatorStorage.set(new AtomicDouble());
        stateNumeratorStorage.set(DenseMatrix.valueOf(numberOfStates, numberOfStates));
        viewNumeratorStorage.set(DenseMatrix.valueOf(numberOfStates, itemSize));
        viewDenominatorStorage.set(DenseVector.valueOf(numberOfStates));
    }

    @Override
    protected void destructEnvironment() {
        // E Step并发计算部分
        alphaStorage.remove();
        betaStorage.remove();
        rhoStorage.remove();
        normContextStorage.remove();
        normStateStorage.remove();
        sumGammaStorage.remove();
        gammaSumStorage.remove();
        binomialStorage.remove();
        multinomialStorage.remove();

        // M Step并发计算部分
        numeratorStorage.remove();
        denominatorStorage.remove();

        probabilityNumeratorStorage.remove();
        probabilityDenominatorStorage.remove();
        stateNumeratorStorage.remove();
        viewNumeratorStorage.remove();
        viewDenominatorStorage.remove();
    }

    /**
     * 准备模型
     */
    private void prepareModel() {
        probabilityNumerator = DenseVector.valueOf(numberOfStates);
        probabilityDenominator = new AtomicDouble();
        stateNumerator = DenseMatrix.valueOf(numberOfStates, numberOfStates);
        stateDenominator = DenseVector.valueOf(numberOfStates);
        viewNumerator = DenseMatrix.valueOf(numberOfStates, itemSize);
        viewDenominator = DenseVector.valueOf(numberOfStates);

        // probabilities => {numberOfStates}
        probabilities = DenseVector.valueOf(numberOfStates);
        // 归一化
        probabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        probabilities.scaleValues(1F / probabilities.getSum(false));
        probabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue((float) Math.log(scalar.getValue()));
        });

        // stateProbabilities = {numberOfStates, numberOfStates}
        stateProbabilities = DenseMatrix.valueOf(numberOfStates, numberOfStates);
        // 归一化
        for (int stateIndex = 0; stateIndex < numberOfStates; stateIndex++) {
            MathVector probabilities = stateProbabilities.getRowVector(stateIndex);
            probabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                scalar.setValue(RandomUtility.randomFloat(1F));
            });
            probabilities.scaleValues(1F / probabilities.getSum(false));
        }
        stateProbabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue((float) Math.log(scalar.getValue()));
        });

        // viewNumerator => {numberOfItems}
        DenseVector viewNumerator = DenseVector.valueOf(itemSize);
        // viewDenominator => sum(sizeOfContexts)
        float viewDenominator = 0F;
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            SparseMatrix dataMatrix = dataMatrixes[userIndex];
            for (MatrixScalar term : dataMatrix) {
                viewNumerator.shiftValue(term.getColumn(), term.getValue());
            }
            viewDenominator += dataMatrix.getRowSize();
        }
        // viewProbabilities => {numberOfStates, numberOfItems}
        viewNumerator.scaleValues(1F / viewDenominator);
        // 保证不为0且归一化
        viewNumerator.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            float value = scalar.getValue();
            scalar.setValue(value == 0F ? nearZero : value);
        });
        viewNumerator.scaleValues(1F / viewNumerator.getSum(false));
        viewProbabilities = DenseMatrix.valueOf(numberOfStates, itemSize);
        for (int stateIndex = 0; stateIndex < numberOfStates; stateIndex++) {
            viewProbabilities.getRowVector(stateIndex).copyVector(viewNumerator);
        }

        // numeratorPsiGamma => {numberOfStates}
        numeratorPsiGamma = DenseVector.valueOf(numberOfStates);
        // numeratorLogGamma=> {numberOfStates}
        numeratorLogGamma = DenseVector.valueOf(numberOfStates);
        // denominatorPsiGamma => {numberOfStates}
        denominatorPsiGamma = DenseVector.valueOf(numberOfStates);
        // denominatorLogGamma => {numberOfStates}
        denominatorLogGamma = DenseVector.valueOf(numberOfStates);
        // psiNumerator => {numberOfStates}
        psiNumerator = DenseVector.valueOf(numberOfStates);
        // nutNumerator => {numberOfStates}
        nutNumerator = DenseVector.valueOf(numberOfStates);
        // next_denominator => {numberOfStates}
        averageDenominator = DenseVector.valueOf(numberOfStates);
        // numerator => {numberOfStates}
        modelNumerator = DenseVector.valueOf(numberOfStates);
        // denominator => {numberOfStates}
        modelDenominator = DenseVector.valueOf(numberOfStates);

        alpha = DenseVector.valueOf(numberOfStates);
        beta = DenseVector.valueOf(numberOfStates);

        gammas = new DenseMatrix[userSize];
        nuts = new DenseVector[userSize];
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            SparseMatrix dataMatrix = dataMatrixes[userIndex];
            int sizeOfContexts = dataMatrix.getRowSize();
            DenseMatrix gamma = DenseMatrix.valueOf(sizeOfContexts, numberOfStates);
            gammas[userIndex] = gamma.setValues(1F);

            DenseVector nut = DenseVector.valueOf(sizeOfContexts);
            nut.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                SparseVector dataVector = dataMatrix.getRowVector(scalar.getIndex());
                scalar.setValue(dataVector.getSum(false));
            });
            nuts[userIndex] = nut;
        }

    }

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        // 上下文维度
        contextField = configuration.getString("data.model.fields.context");
        contextDimension = model.getQualityInner(contextField);
        numberOfStates = configuration.getInteger("recommender.hmm.state.number");
        probabilityRegularization = configuration.getFloat("recommender.probability.regularization", 100F);
        stateRegularization = configuration.getFloat("recommender.state.regularization", 100F);
        viewRegularization = configuration.getFloat("recommender.view.regularization", 100F);

        // 检查参数配置
        if (probabilityRegularization < numberOfStates || stateRegularization < numberOfStates || viewRegularization < itemSize) {
            throw new IllegalArgumentException();
        }

        // 按照上下文划分数据
        dataMatrixes = new SparseMatrix[userSize];
        contextSize = 0;

        MemoryQualityAttribute attribute = (MemoryQualityAttribute) space.getQualityAttribute(contextField);
        Object[] levels = attribute.getDatas();
        Table<Integer, Integer, Float> table = HashBasedTable.create();
        Table<Integer, Integer, Float> data = HashBasedTable.create();

        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            DataModule module = models[userIndex];
            for (DataInstance instance : module) {
                int rowKey = (Integer) levels[instance.getQualityFeature(contextDimension)];
                int columnKey = instance.getQualityFeature(itemDimension);
                Float count = table.get(rowKey, columnKey);
                table.put(rowKey, columnKey, count == null ? 1 : ++count);
            }

            ArrayList<Integer> keys = new ArrayList<>(table.rowKeySet());
            Collections.sort(keys);
            int index = 0;
            for (Integer key : keys) {
                for (Entry<Integer, Float> term : table.row(key).entrySet()) {
                    data.put(index, term.getKey(), term.getValue());
                }
                index++;
            }
            table.clear();

            // 使用稀疏矩阵
            SparseMatrix matrix = SparseMatrix.valueOf(keys.size(), itemSize, data);
            if (contextSize < matrix.getRowSize()) {
                contextSize = matrix.getRowSize();
            }
            dataMatrixes[userIndex] = matrix;

            data.clear();
            System.out.println(userIndex + " " + matrix.getRowSize() + " " + matrix.getColumnSize());
        }

        // 准备模型
        prepareModel();
    }

    /**
     * 范数
     * 
     * @param vector
     * @return
     */
    private float calculateNorm(DenseVector vector) {
        // log(sum(exp(vector))) 用于保持凸性
        // log是对数函数,exp是指数函数
        float maximum = Float.NEGATIVE_INFINITY;
        for (VectorScalar term : vector) {
            if (maximum < term.getValue()) {
                maximum = term.getValue();
            }
        }
        float sum = 0F;
        for (VectorScalar term : vector) {
            sum += Math.exp(term.getValue() - maximum);
        }
        return (float) (maximum + Math.log(sum));
    }

    /**
     * 计算辐射概率矩阵
     * 
     * @param matrix
     * @return
     */
    private DenseMatrix calculateEmissionProbabilities(int userIndex, SparseMatrix matrix) {
        // matrix => {sizeOfContexts, numberOfItems}

        // first-fourth=> {sizeOfContexts}
        int sizeOfContexts = matrix.getRowSize();
        DenseVector sumVector = nuts[userIndex];
        DenseVector sumGammaVector = DenseVector.valueOf(sizeOfContexts, sumGammaStorage.get());
        DenseVector gammaSumVector = DenseVector.valueOf(sizeOfContexts, gammaSumStorage.get());

        for (int contextIndex = 0; contextIndex < sizeOfContexts; contextIndex++) {
            sumGammaVector.setValue(contextIndex, GammaUtility.logGamma(sumVector.getValue(contextIndex) + 1F));

            SparseVector vector = matrix.getRowVector(contextIndex);
            float value = 0F;
            for (VectorScalar term : vector) {
                value += GammaUtility.logGamma(term.getValue() + 1F);
            }
            value += (GammaUtility.logGamma(1F) * (itemSize - vector.getElementSize()));
            gammaSumVector.setValue(contextIndex, value);
        }

        DenseVector logAlpha = DenseVector.valueOf(numberOfStates, alphaStorage.get());
        logAlpha.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(GammaUtility.logGamma(alpha.getValue(scalar.getIndex())));
        });
        DenseVector logBeta = DenseVector.valueOf(numberOfStates, betaStorage.get());
        logBeta.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue((float) Math.log(beta.getValue(scalar.getIndex()) + 1F));
        });

        // 计算负二项分布矩阵
        // binomial => {sizeOfContexts, numberOfStates}
        DenseMatrix binomial = DenseMatrix.valueOf(matrix.getRowSize(), numberOfStates, binomialStorage.get());
        binomial.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            int row = scalar.getRow();
            int column = scalar.getColumn();
            float value = GammaUtility.logGamma(sumVector.getValue(row) + alpha.getValue(column));
            value -= sumGammaVector.getValue(row);
            value += (sumVector.getValue(row) * Math.log(beta.getValue(column)));
            value -= ((sumVector.getValue(row) + alpha.getValue(column)) * logBeta.getValue(column));
            value -= logAlpha.getValue(column);
            scalar.setValue(value);
        });

        sumGammaVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            int index = scalar.getIndex();
            float value = scalar.getValue();
            scalar.setValue(value - gammaSumVector.getValue(index));
        });

        // 计算多项分布矩阵
        // multinomial => {sizeOfContexts, numberOfStates}
        DenseMatrix multinomial = DenseMatrix.valueOf(matrix.getRowSize(), numberOfStates, multinomialStorage.get());
        multinomial.dotProduct(matrix, false, viewProbabilities, true, MathCalculator.SERIAL);
        for (int index = 0; index < multinomial.getRowSize(); index++) {
            multinomial.getRowVector(index).shiftValues(sumGammaVector.getValue(index));
        }

        // emission => {sizeOfContexts, numberOfStates}
        binomial.addMatrix(multinomial, false);
        DenseMatrix emission = binomial;
        return emission;
    }

    /**
     * 计算 gamma and rho.
     * 
     * @param matrix
     * @return
     */
    private void calculateGammaRho(int userIndex, SparseMatrix matrix) {
        // Formula 1.1
        DenseMatrix logEmission = calculateEmissionProbabilities(userIndex, matrix);

        // calculateAlphaBeta
        int sizeOfContexts = logEmission.getRowSize();
        // TODO 此处按照建议,可以考虑改为一个极大的负数作为logAlpha和logBeta的初始化值.具体效果待验证.
        // logAlpha => {sizeOfContexts, numberOfStates}
        DenseMatrix logAlpha = DenseMatrix.valueOf(sizeOfContexts, numberOfStates, alphaStorage.get());
        logAlpha.setValues(0F);
        // logBeta => {sizeOfContexts, numberOfStates}
        DenseMatrix logBeta = DenseMatrix.valueOf(sizeOfContexts, numberOfStates, betaStorage.get());
        logBeta.setValues(0F);
        // contextNorm => {sizeOfContexts}
        DenseVector contextNorm = DenseVector.valueOf(sizeOfContexts, normContextStorage.get());
        // stateNorm => {numberOfStates}
        DenseVector stateNorm = DenseVector.valueOf(numberOfStates, normStateStorage.get());

        // Formula 1.2
        DenseVector emission = logEmission.getRowVector(0);
        logAlpha.getRowVector(0).iterateElement(MathCalculator.SERIAL, (scalar) -> {
            int index = scalar.getIndex();
            scalar.setValue(probabilities.getValue(index) + emission.getValue(index));
        });
        float norm = calculateNorm(logAlpha.getRowVector(0));
        contextNorm.setValue(0, norm);
        logAlpha.getRowVector(0).shiftValues(-norm);

        // Formula 1.3
        for (int context = 1; context < sizeOfContexts; context++) {
            int contextIndex = context;
            for (int state = 0; state < numberOfStates; state++) {
                stateNorm.copyVector(stateProbabilities.getColumnVector(state));
                stateNorm.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                    int index = scalar.getIndex();
                    float value = scalar.getValue();
                    scalar.setValue(value + logAlpha.getValue(contextIndex - 1, index));
                });
                norm = calculateNorm(stateNorm) + logEmission.getValue(context, state);
                logAlpha.setValue(context, state, norm);
            }
            norm = calculateNorm(logAlpha.getRowVector(context));
            contextNorm.setValue(context, norm);
            logAlpha.getRowVector(context).shiftValues(-norm);
        }

        // Formula 1.4
        for (int context = sizeOfContexts - 2; context > -1; context--) {
            int contextIndex = context;
            for (int state = 0; state < numberOfStates; state++) {
                stateNorm.copyVector(stateProbabilities.getRowVector(state));
                stateNorm.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                    int index = scalar.getIndex();
                    float value = scalar.getValue();
                    scalar.setValue(value + logBeta.getValue(contextIndex + 1, index) + logEmission.getValue(contextIndex + 1, index));
                });
                norm = calculateNorm(stateNorm);
                logBeta.setValue(context, state, norm);
            }
            logBeta.getRowVector(context).shiftValues(-contextNorm.getValue(context + 1));
        }

        // Formula 1.5
        gammas[userIndex].iterateElement(MathCalculator.SERIAL, (scalar) -> {
            int row = scalar.getRow();
            int column = scalar.getColumn();
            scalar.setValue((float) (Math.exp(logAlpha.getValue(row, column) + logBeta.getValue(row, column))));
        });

        // Formula 1.6
        logEmission.addMatrix(logBeta, false);
        DenseMatrix rho = DenseMatrix.valueOf(numberOfStates, numberOfStates, rhoStorage.get());
        DenseMatrix stateNumeratorCache = stateNumeratorStorage.get();
        for (int context = 0; context < sizeOfContexts - 1; context++) {
            int contextIndex = context;
            for (int state = 0; state < numberOfStates; state++) {
                int stateIndex = state;
                rho.getRowVector(state).iterateElement(MathCalculator.SERIAL, (scalar) -> {
                    int index = scalar.getIndex();
                    scalar.setValue(stateProbabilities.getValue(stateIndex, index) + logEmission.getValue(contextIndex + 1, index) + logAlpha.getValue(contextIndex, stateIndex));
                });
            }
            float normValue = contextNorm.getValue(context + 1);
            rho.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                scalar.setValue((float) Math.exp(scalar.getValue() - normValue));
            });
            stateNumeratorCache.addMatrix(rho, false);
        }
    }

    @Override
    protected boolean isConverged(int iterationStep) {
        // calculate the expected likelihood.
        // Formula 1.12
        float likelihood = 0F;
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            // gamma => {sizeOfContexts, numberOfStates}
            DenseMatrix gamma = gammas[userIndex];
            if (gamma.getRowSize() == 0) {
                // 处理用户sizeOfContexts == 0的情况
                continue;
            }
            float probability = 0F;
            for (int stateIndex = 0; stateIndex < numberOfStates; stateIndex++) {
                probability += (gamma.getValue(0, stateIndex) * probabilities.getValue(stateIndex));
            }

            // // rho => {sizeOfContexts - 1, numberOfStates, numberOfStates}
            // DenseMatrix rho = logRhos[userIndex];
            // double state = rho.calculate((row, column, value) -> {
            // return value * stateProbabilities.getTermValue(row, column);
            // }).sum();

            // // binomial => {sizeOfContexts, numberOfStates}
            // DenseMatrix binomial =
            // calculateNegativeBinomial(dataMatrixes[userIndex]);
            // double negative = binomial.calculate((row, column, value) -> {
            // return value * gamma.getTermValue(row, column);
            // }).sum();
            //
            // // multinomial => {sizeOfContexts, numberOfStates}
            // DenseMatrix multinomial =
            // calculateMultinomial(dataMatrixes[userIndex]);
            // double positive = multinomial.calculate((row, column, value) -> {
            // return value * gamma.getTermValue(row, column);
            // }).sum();

            likelihood += probability;
            if (Float.isNaN(likelihood)) {
                throw new IllegalStateException();
            }
        }

        // 是否收敛
        System.out.println(iterationStep + " " + likelihood);
        float deltaLoss = likelihood - currentError;
        if (iterationStep > 1 && (deltaLoss < 0.1F)) {
            return true;
        }
        currentError = likelihood;
        return false;
    }

    /**
     * 计算隐马尔可夫模型
     * 
     * @param dataMatrixes
     * @return
     */
    private void calculateModel() {
        EnvironmentContext context = EnvironmentContext.getContext();
        psiNumerator.setValues(0F);
        nutNumerator.setValues(0F);
        averageDenominator.setValues(0F);

        // TODO 此处可以并发
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            DenseMatrix gamma = gammas[userIndex];
            DenseVector nut = nuts[userIndex];

            nutNumerator.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                int index = scalar.getIndex();
                float value = scalar.getValue();
                for (VectorScalar term : gamma.getColumnVector(index)) {
                    value += (term.getValue() * nut.getValue(term.getIndex()));
                }
                scalar.setValue(value);
            });

            psiNumerator.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                int index = scalar.getIndex();
                float value = scalar.getValue();
                for (VectorScalar term : gamma.getColumnVector(index)) {
                    // TODO 减少PolyGamma.psigamma计算
                    value += (term.getValue() * GammaUtility.digamma(nut.getValue(term.getIndex())));
                }
                scalar.setValue(value);
            });

            averageDenominator.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                int index = scalar.getIndex();
                float value = scalar.getValue();
                scalar.setValue(value + gamma.getColumnVector(index).getSum(false));
            });
        }

        beta.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            int index = scalar.getIndex();
            float value = scalar.getValue();
            scalar.setValue(nutNumerator.getValue(index) / averageDenominator.getValue(index));
        });
        alpha.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            int index = scalar.getIndex();
            float value = scalar.getValue();
            scalar.setValue((float) (0.5F / (Math.log(beta.getValue(index)) - psiNumerator.getValue(index) / averageDenominator.getValue(index))));
        });

        for (int sampleIndex = 0; sampleIndex < sampleSize; sampleIndex++) {
            modelNumerator.setValues(0F);
            modelDenominator.setValues(0F);
            {
                context.doAlgorithmByEvery(() -> {
                    numeratorStorage.get().setValues(0F);
                    denominatorStorage.get().setValues(0F);
                });
            }

            for (int stateIndex = 0; stateIndex < numberOfStates; stateIndex++) {
                numeratorPsiGamma.setValue(stateIndex, GammaUtility.digamma(alpha.getValue(stateIndex)));
                numeratorLogGamma.setValue(stateIndex, (float) (Math.log(beta.getValue(stateIndex) / alpha.getValue(stateIndex) + 1F)));
                denominatorPsiGamma.setValue(stateIndex, GammaUtility.trigamma(alpha.getValue(stateIndex)));
                denominatorLogGamma.setValue(stateIndex, (float) (1F / (beta.getValue(stateIndex) + alpha.getValue(stateIndex))));
            }

            {
                // 并发计算
                CountDownLatch latch = new CountDownLatch(userSize);
                for (int userIndex = 0; userIndex < userSize; userIndex++) {
                    DenseMatrix gamma = gammas[userIndex];
                    DenseVector nut = nuts[userIndex];
                    context.doAlgorithmByAny(userIndex, () -> {
                        // numeratorMatrix => {sizeOfContexts, numberOfStates}
                        DenseVector numeratorCache = numeratorStorage.get();
                        // denominatorMatrix => {sizeOfContexts, numberOfStates}
                        DenseVector denominatorCache = denominatorStorage.get();
                        for (MatrixScalar term : gamma) {
                            int row = term.getRow();
                            int column = term.getColumn();
                            numeratorCache.shiftValue(column, term.getValue() * (GammaUtility.digamma(nut.getValue(row) + alpha.getValue(column)) - numeratorPsiGamma.getValue(column) - numeratorLogGamma.getValue(column)));
                            denominatorCache.shiftValue(column, term.getValue() * (GammaUtility.trigamma(nut.getValue(row) + alpha.getValue(column)) - denominatorPsiGamma.getValue(column) - denominatorLogGamma.getValue(column) + (1F / alpha.getValue(column))));
                        }
                        latch.countDown();
                    });
                }
                try {
                    latch.await();
                } catch (Exception exception) {
                    throw new RecommendException(exception);
                }
            }

            {
                context.doAlgorithmByEvery(() -> {
                    synchronized (modelNumerator) {
                        modelNumerator.addVector(numeratorStorage.get());
                    }
                    synchronized (modelDenominator) {
                        modelDenominator.addVector(denominatorStorage.get());
                    }
                });
            }

            // TODO 此处相当于学习率
            modelDenominator.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                int index = scalar.getIndex();
                float value = scalar.getValue();
                value = (value == 0D ? nearZero : value);
                scalar.setValue(modelNumerator.getValue(index) / value);
            });
            boolean isBreak = false;
            for (VectorScalar term : alpha) {
                if (term.getValue() <= modelDenominator.getValue(term.getIndex())) {
                    isBreak = true;
                    break;
                }
            }
            if (isBreak) {
                break;
            }
            alpha.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                int index = scalar.getIndex();
                float value = scalar.getValue();
                scalar.setValue(value - modelDenominator.getValue(index));
            });
            if (!checkModel(alpha)) {
                throw new IllegalStateException();
            }
        }

        beta.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            int index = scalar.getIndex();
            float value = scalar.getValue();
            scalar.setValue(value / alpha.getValue(index));
        });
    }

    @Override
    protected void doPractice() {
        calculateModel();

        super.doPractice();

        // Formula 1.13 and Formula 1.14
        norms = DenseMatrix.valueOf(userSize, numberOfStates);
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            gammas[userIndex].iterateElement(MathCalculator.SERIAL, (scalar) -> {
                scalar.setValue((float) Math.log(scalar.getValue()));
            });
            for (int stateIndex = 0; stateIndex < numberOfStates; stateIndex++) {
                int state = stateIndex;
                // gamma => {numberOfStates}
                DenseMatrix gamma = gammas[userIndex];
                // 处理用户sizeOfContexts == 0的情况
                DenseVector norm = DenseVector.copyOf(gamma.getRowSize() == 0 ? probabilities : gamma.getRowVector(gamma.getRowSize() - 1));
                norm.addVector(stateProbabilities.getColumnVector(state));
                norms.setValue(userIndex, stateIndex, calculateNorm(norm));
            }
        }

        viewProbabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            int row = scalar.getRow();
            int column = scalar.getColumn();
            float value = scalar.getValue();
            scalar.setValue((float) (Math.log(Math.exp(value) * beta.getValue(row) + 1D) * alpha.getValue(row)));
        });
    }

    @Override
    protected void eStep() {
        EnvironmentContext context = EnvironmentContext.getContext();
        // 并发计算
        CountDownLatch latch = new CountDownLatch(userSize);
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            int user = userIndex;
            context.doAlgorithmByAny(userIndex, () -> {
                calculateGammaRho(user, dataMatrixes[user]);
                latch.countDown();
            });
        }
        try {
            latch.await();
        } catch (Exception exception) {
            throw new RecommendException(exception);
        }
    }

    @Override
    protected void mStep() {
        EnvironmentContext context = EnvironmentContext.getContext();
        probabilityNumerator.setValues(0F);
        probabilityDenominator.set(0D);
        stateNumerator.setValues(0F);
        viewNumerator.setValues(0F);
        viewDenominator.setValues(0F);

        {
            context.doAlgorithmByEvery(() -> {
                probabilityNumeratorStorage.get().setValues(0F);
                probabilityDenominatorStorage.get().set(0D);
                stateNumerator.addMatrix(stateNumeratorStorage.get(), false);
                stateNumeratorStorage.get().setValues(0F);
                viewNumeratorStorage.get().setValues(0F);
                viewDenominatorStorage.get().setValues(0F);
            });
        }

        stateDenominator.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            int index = scalar.getIndex();
            scalar.setValue(stateNumerator.getRowVector(index).getSum(false));
        });

        {
            // 并发计算
            CountDownLatch latch = new CountDownLatch(userSize);
            for (int userIndex = 0; userIndex < userSize; userIndex++) {
                // gamma => {sizeOfContexts, numberOfStates}
                DenseMatrix gamma = gammas[userIndex];
                if (gamma.getRowSize() == 0) {
                    // 处理用户sizeOfContexts == 0的情况
                    latch.countDown();
                    continue;
                }
                DenseVector nut = nuts[userIndex];
                SparseMatrix dataMatrix = dataMatrixes[userIndex];
                context.doAlgorithmByAny(userIndex, () -> {
                    MathVector gammaVector = gamma.getRowVector(0);
                    probabilityNumeratorStorage.get().addVector(gammaVector);
                    probabilityDenominatorStorage.get().addAndGet(gammaVector.getSum(false));
                    // viewNumerator => {numberOfStates, numberOfItems}
                    // 利用稀疏矩阵减少计算.
                    for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                        if (dataMatrix.getColumnScope(itemIndex) > 0) {
                            SparseVector itemVector = dataMatrix.getColumnVector(itemIndex);
                            viewNumeratorStorage.get().getColumnVector(itemIndex).iterateElement(MathCalculator.SERIAL, (scalar) -> {
                                int index = scalar.getIndex();
                                float value = scalar.getValue();
                                for (VectorScalar term : itemVector) {
                                    value += (gamma.getValue(term.getIndex(), index) * term.getValue());
                                }
                                scalar.setValue(value);
                            });
                        }
                    }
                    // viewDenominator => {numberOfStates}
                    viewDenominatorStorage.get().iterateElement(MathCalculator.SERIAL, (scalar) -> {
                        int index = scalar.getIndex();
                        float value = scalar.getValue();
                        for (VectorScalar term : nut) {
                            value += (gamma.getValue(term.getIndex(), index) * term.getValue());
                        }
                        scalar.setValue(value);
                    });
                    latch.countDown();
                });
            }
            try {
                latch.await();
            } catch (Exception exception) {
                throw new RecommendException(exception);
            }
        }

        {
            context.doAlgorithmByEvery(() -> {
                synchronized (probabilityNumerator) {
                    probabilityNumerator.addVector(probabilityNumeratorStorage.get());
                }
                synchronized (probabilityDenominator) {
                    probabilityDenominator.addAndGet(probabilityDenominatorStorage.get().get());
                }
                synchronized (viewNumerator) {
                    viewNumerator.addMatrix(viewNumeratorStorage.get(), false);
                }
                synchronized (viewDenominator) {
                    viewDenominator.addVector(viewDenominatorStorage.get());
                }
            });
        }

        // Formula 1.7
        probabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            int index = scalar.getIndex();
            float value = probabilityNumerator.getValue(index) + (probabilityRegularization / numberOfStates - 1F);
            value = (float) (value / (probabilityDenominator.get() + probabilityRegularization - numberOfStates));
            value = (float) Math.log(value);
            scalar.setValue(value);
        });

        // Formula 1.8
        stateProbabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            int row = scalar.getRow();
            int column = scalar.getColumn();
            float value = stateNumerator.getValue(row, column) + (stateRegularization / numberOfStates - 1F);
            value = (float) (value / (stateDenominator.getValue(row) + stateRegularization - numberOfStates));
            value = (float) Math.log(value);
            scalar.setValue(value);
        });

        // Formula 1.9
        viewProbabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            int row = scalar.getRow();
            int column = scalar.getColumn();
            float value = viewNumerator.getValue(row, column) + (viewRegularization / itemSize - 1F);
            value = (float) (value / (viewDenominator.getValue(row) + viewRegularization - itemSize));
            value = (float) Math.log(value);
            scalar.setValue(value);
        });

        // Formula 1.10 and Formula 1.11
        calculateModel();
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        float score = 0F;
        for (int state = 0; state < numberOfStates; state++) {
            score += Math.exp(norms.getValue(userIndex, state) - viewProbabilities.getValue(state, itemIndex));
        }
        score = 1F - score;
        instance.setQuantityMark(score);
    }

}
