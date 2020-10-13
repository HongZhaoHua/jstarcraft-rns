package com.jstarcraft.rns.model;

import java.util.Map.Entry;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.algorithm.probability.QuantityProbability;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.vector.ArrayVector;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.common.option.Option;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.model.exception.ModelException;

/**
 * Factorization Machine Recommender
 *
 * Rendle, Steffen, et al., <strong>Fast Context-aware Recommendations with
 * Factorization Machines</strong>, SIGIR, 2011.
 *
 * @author Tang Jiaxi and Ma Chen
 */
// TODO 论文中需要支持组合特征(比如:历史评价过的电影),现在的代码并没有实现.
public abstract class FactorizationMachineModel extends EpocheModel {

    /** 是否自动调整学习率 */
    protected boolean isLearned;

    /** 衰减率 */
    protected float learnDecay;

    /**
     * learn rate, maximum learning rate
     */
    protected float learnRatio, learnLimit;

    protected DataModule marker;

    /**
     * global bias
     */
    protected float globalBias;
    /**
     * appender vector size: number of users + number of items + number of
     * contextual conditions
     */
    protected int featureSize;
    /**
     * number of factors
     */
    protected int factorSize;

    /**
     * weight vector
     */
    protected DenseVector weightVector; // p
    /**
     * parameter matrix(featureFactors)
     */
    protected DenseMatrix featureFactors; // p x k
    /**
     * parameter matrix(rateFactors)
     */
    protected DenseMatrix actionFactors; // n x k
    /**
     * regularization term for weight and factors
     */
    protected float biasRegularization, weightRegularization, factorRegularization;

    /**
     * init mean
     */
    protected float initMean;

    /**
     * init standard deviation
     */
    protected float initStd;

    protected QuantityProbability distribution;

    protected int[] dimensionSizes;

    @Override
    public void prepare(Option configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);

        isLearned = configuration.getBoolean("recommender.learnrate.bolddriver", false);
        learnDecay = configuration.getFloat("recommender.learnrate.decay", 1.0f);
        learnRatio = configuration.getFloat("recommender.iterator.learnrate", 0.01f);
        learnLimit = configuration.getFloat("recommender.iterator.learnrate.maximum", 1000.0f);

        maximumScore = configuration.getFloat("recommender.recommender.maxrate", 12F);
        minimumScore = configuration.getFloat("recommender.recommender.minrate", 0F);

        factorSize = configuration.getInteger("recommender.factor.number");

        // init all weight with zero
        globalBias = 0;

        // init factors with small value
        // TODO 此处需要重构
        initMean = configuration.getFloat("recommender.init.mean", 0F);
        initStd = configuration.getFloat("recommender.init.std", 0.1F);

        biasRegularization = configuration.getFloat("recommender.fm.regw0", 0.01F);
        weightRegularization = configuration.getFloat("recommender.fm.regW", 0.01F);
        factorRegularization = configuration.getFloat("recommender.fm.regF", 10F);

        // TODO 暂时不支持连续特征,考虑将连续特征离散化.
        this.marker = model;
        dimensionSizes = new int[marker.getQualityOrder()];

        // TODO 考虑重构,在AbstractRecommender初始化
        actionSize = marker.getSize();
        // initialize the parameters of FM
        // TODO 此处需要重构,外部索引与内部索引的映射转换
        for (int orderIndex = 0, orderSize = marker.getQualityOrder() + marker.getQuantityOrder(); orderIndex < orderSize; orderIndex++) {
            Entry<Integer, KeyValue<String, Boolean>> term = marker.getOuterKeyValue(orderIndex);
            if (term.getValue().getValue()) {
                // 处理离散维度
                dimensionSizes[marker.getQualityInner(term.getValue().getKey())] = space.getQualityAttribute(term.getValue().getKey()).getSize();
                featureSize += dimensionSizes[marker.getQualityInner(term.getValue().getKey())];
            } else {
                // 处理连续维度
            }
        }
        weightVector = DenseVector.valueOf(featureSize);
        distribution = new QuantityProbability(JDKRandomGenerator.class, 0, NormalDistribution.class, initMean, initStd);
        featureFactors = DenseMatrix.valueOf(featureSize, factorSize);
        featureFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });
    }

    /**
     * 获取特征向量
     * 
     * <pre>
     * 实际为One Hot Encoding(一位有效编码)
     * 详细原理与使用参考:http://blog.csdn.net/pipisorry/article/details/61193868
     * </pre>
     * 
     * @param featureIndexes
     * @return
     */
    protected MathVector getFeatureVector(DataInstance instance) {
        int orderSize = instance.getQualityOrder();
        int[] keys = new int[orderSize];
        int cursor = 0;
        for (int orderIndex = 0; orderIndex < orderSize; orderIndex++) {
            keys[orderIndex] += cursor + instance.getQualityFeature(orderIndex);
            cursor += dimensionSizes[orderIndex];
        }
        ArrayVector vector = new ArrayVector(featureSize, keys);
        vector.setValues(1F);
        return vector;
    }

    /**
     * Predict the rating given a sparse appender vector.
     * 
     * @param userIndex     user Id
     * @param itemIndex     item Id
     * @param featureVector the given vector to predict.
     *
     * @return predicted rating
     * @throws ModelException if error occurs
     */
    protected float predict(DefaultScalar scalar, MathVector featureVector) {
        float value = 0;
        // global bias
        value += globalBias;
        // 1-way interaction
        value += scalar.dotProduct(weightVector, featureVector).getValue();

        // 2-way interaction
        for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
            float scoreSum = 0F;
            float predictSum = 0F;
            for (VectorScalar vectorTerm : featureVector) {
                float featureValue = vectorTerm.getValue();
                int featureIndex = vectorTerm.getIndex();
                float predictValue = featureFactors.getValue(featureIndex, factorIndex);

                scoreSum += predictValue * featureValue;
                predictSum += predictValue * predictValue * featureValue * featureValue;
            }
            value += (scoreSum * scoreSum - predictSum) / 2F;
        }

        return value;
    }

    @Override
    public void predict(DataInstance instance) {
        DefaultScalar scalar = DefaultScalar.getInstance();
        // TODO 暂时不支持连续特征,考虑将连续特征离散化.
        MathVector featureVector = getFeatureVector(instance);
        instance.setQuantityMark(predict(scalar, featureVector));
    }

}
