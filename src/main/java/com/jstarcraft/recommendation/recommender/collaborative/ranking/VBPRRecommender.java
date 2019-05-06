package com.jstarcraft.recommendation.recommender.collaborative.ranking;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.ArrayVector;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.recommendation.configurator.Configuration;
import com.jstarcraft.recommendation.recommender.MatrixFactorizationRecommender;
import com.jstarcraft.recommendation.utility.LogisticUtility;

/**
 * 
 * VBPR推荐器
 * 
 * <pre>
 * Bayesian Personalized Ranking for Non-Uniformly Sampled Items
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class VBPRRecommender extends MatrixFactorizationRecommender {

    /**
     * items biases
     */
    private DenseVector itemBiases;

    private float biasRegularization;

    private double featureRegularization;

    private int numberOfFeatures;
    private DenseMatrix userFeatures;
    private DenseVector itemFeatures;
    private DenseMatrix featureFactors;

    private Table<Integer, Integer, Float> featureTable;
    private DenseMatrix factorMatrix;
    private DenseVector featureVector;

    /** 采样比例 */
    private int sampleRatio;

    @Override
    public void prepare(Configuration configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);

        // TODO 此处代码可以消除(使用常量Marker代替或者使用binarize.threshold)
        for (MatrixScalar term : trainMatrix) {
            term.setValue(1F);
        }

        biasRegularization = configuration.getFloat("rec.bias.regularization", 0.1F);
        // TODO 此处应该修改为配置或者动态计算.
        numberOfFeatures = 4096;
        featureRegularization = 1000;
        sampleRatio = configuration.getInteger("rec.vbpr.alpha", 5);

        itemBiases = DenseVector.valueOf(numberOfItems);
        itemBiases.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });

        itemFeatures = DenseVector.valueOf(numberOfFeatures);
        itemFeatures.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });

        userFeatures = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
        userFeatures.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });

        featureFactors = DenseMatrix.valueOf(numberOfFactors, numberOfFeatures);
        featureFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });

        float minimumValue = Float.MAX_VALUE;
        float maximumValue = Float.MIN_VALUE;
        featureTable = HashBasedTable.create();
        DataModule featureModel = space.getModule("article");
        String articleField = configuration.getString("data.model.fields.article");
        String featureField = configuration.getString("data.model.fields.feature");
        String degreeField = configuration.getString("data.model.fields.degree");
        int articleDimension = featureModel.getQualityInner(articleField);
        int featureDimension = featureModel.getQualityInner(featureField);
        int degreeDimension = featureModel.getQuantityInner(degreeField);
        for (DataInstance instance : featureModel) {
            int itemIndex = instance.getQualityFeature(articleDimension);
            int featureIndex = instance.getQualityFeature(featureDimension);
            float featureValue = instance.getQuantityFeature(degreeDimension);
            if (featureValue < minimumValue) {
                minimumValue = featureValue;
            }
            if (featureValue > maximumValue) {
                maximumValue = featureValue;
            }
            featureTable.put(itemIndex, featureIndex, featureValue);
        }
        for (Cell<Integer, Integer, Float> cell : featureTable.cellSet()) {
            float value = (cell.getValue() - minimumValue) / (maximumValue - minimumValue);
            featureTable.put(cell.getRowKey(), cell.getColumnKey(), value);
        }
        factorMatrix = DenseMatrix.valueOf(numberOfFactors, numberOfItems);
        factorMatrix.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });
    }

    @Override
    protected void doPractice() {
        DefaultScalar scalar = DefaultScalar.getInstance();
        DenseVector factorVector = DenseVector.valueOf(featureFactors.getRowSize());
        ArrayVector[] featureVectors = new ArrayVector[numberOfItems];
        for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
            Map<Integer, Float> keyValues = new TreeMap<>(featureTable.row(itemIndex));
            int[] featureIndexes = new int[keyValues.size()];
            float[] featureValues = new float[keyValues.size()];
            int index = 0;
            for (Entry<Integer, Float> keyValue : keyValues.entrySet()) {
                featureIndexes[index] = keyValue.getKey();
                featureValues[index] = keyValue.getValue();
                index++;
            }
            featureVectors[itemIndex] = new ArrayVector(numberOfFeatures, featureIndexes, featureValues);
        }
        float[] featureValues = new float[numberOfFeatures];

        for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
            totalLoss = 0F;
            for (int sampleIndex = 0, numberOfSamples = numberOfUsers * sampleRatio; sampleIndex < numberOfSamples; sampleIndex++) {
                // randomly draw (u, i, j)
                int userKey, positiveItemKey, negativeItemKey;
                while (true) {
                    userKey = RandomUtility.randomInteger(numberOfUsers);
                    SparseVector userVector = trainMatrix.getRowVector(userKey);
                    if (userVector.getElementSize() == 0) {
                        continue;
                    }
                    positiveItemKey = userVector.getIndex(RandomUtility.randomInteger(userVector.getElementSize()));
                    negativeItemKey = RandomUtility.randomInteger(numberOfItems - userVector.getElementSize());
                    for (VectorScalar term : userVector) {
                        if (negativeItemKey >= term.getIndex()) {
                            negativeItemKey++;
                        } else {
                            break;
                        }
                    }
                    break;
                }
                int userIndex = userKey, positiveItemIndex = positiveItemKey, negativeItemIndex = negativeItemKey;
                ArrayVector positiveItemVector = featureVectors[positiveItemIndex];
                ArrayVector negativeItemVector = featureVectors[negativeItemIndex];
                // update parameters
                float positiveScore = predict(userIndex, positiveItemIndex, scalar.dotProduct(itemFeatures, positiveItemVector).getValue(), factorVector.dotProduct(featureFactors, false, positiveItemVector, MathCalculator.SERIAL));
                float negativeScore = predict(userIndex, negativeItemIndex, scalar.dotProduct(itemFeatures, negativeItemVector).getValue(), factorVector.dotProduct(featureFactors, false, negativeItemVector, MathCalculator.SERIAL));
                float error = LogisticUtility.getValue(positiveScore - negativeScore);
                totalLoss += (float) -Math.log(error);
                // update bias
                float positiveBias = itemBiases.getValue(positiveItemIndex), negativeBias = itemBiases.getValue(negativeItemIndex);
                itemBiases.shiftValue(positiveItemIndex, learnRate * (error - biasRegularization * positiveBias));
                itemBiases.shiftValue(negativeItemIndex, learnRate * (-error - biasRegularization * negativeBias));
                totalLoss += biasRegularization * positiveBias * positiveBias + biasRegularization * negativeBias * negativeBias;
                for (VectorScalar term : positiveItemVector) {
                    featureValues[term.getIndex()] = term.getValue();
                }
                for (VectorScalar term : negativeItemVector) {
                    featureValues[term.getIndex()] -= term.getValue();
                }
                // update user/item vectors
                // 按照因子切割任务实现并发计算.
                // CountDownLatch factorLatch = new
                // CountDownLatch(numberOfFactors);
                for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
                    float userFactor = userFactors.getValue(userIndex, factorIndex);
                    float positiveItemFactor = itemFactors.getValue(positiveItemIndex, factorIndex);
                    float negativeItemFactor = itemFactors.getValue(negativeItemIndex, factorIndex);
                    userFactors.shiftValue(userIndex, factorIndex, learnRate * (error * (positiveItemFactor - negativeItemFactor) - userRegularization * userFactor));
                    itemFactors.shiftValue(positiveItemIndex, factorIndex, learnRate * (error * (userFactor) - itemRegularization * positiveItemFactor));
                    itemFactors.shiftValue(negativeItemIndex, factorIndex, learnRate * (error * (-userFactor) - itemRegularization * negativeItemFactor));
                    totalLoss += userRegularization * userFactor * userFactor + itemRegularization * positiveItemFactor * positiveItemFactor + itemRegularization * negativeItemFactor * negativeItemFactor;

                    float userFeature = userFeatures.getValue(userIndex, factorIndex);
                    DenseVector featureVector = featureFactors.getRowVector(factorIndex);
                    userFeatures.shiftValue(userIndex, factorIndex, learnRate * (error * (scalar.dotProduct(featureVector, positiveItemVector).getValue() - scalar.dotProduct(featureVector, negativeItemVector).getValue()) - userRegularization * userFeature));
                    totalLoss += userRegularization * userFeature * userFeature;
                    featureVector.iterateElement(MathCalculator.SERIAL, (element) -> {
                        int index = element.getIndex();
                        float value = element.getValue();
                        totalLoss += featureRegularization * value * value;
                        value += learnRate * (error * userFeature * featureValues[index] - featureRegularization * value);
                        element.setValue(value);
                    });
                }
                // 按照特征切割任务实现并发计算.
                itemFeatures.iterateElement(MathCalculator.SERIAL, (element) -> {
                    int index = element.getIndex();
                    float value = element.getValue();
                    totalLoss += featureRegularization * value * value;
                    value += learnRate * (featureValues[index] - featureRegularization * value);
                    element.setValue(value);
                });
                // try {
                // factorLatch.await();
                // } catch (Exception exception) {
                // throw new LibrecException(exception);
                // }
                for (VectorScalar term : positiveItemVector) {
                    featureValues[term.getIndex()] = 0F;
                }
                for (VectorScalar term : negativeItemVector) {
                    featureValues[term.getIndex()] -= 0F;
                }
            }

            if (isConverged(iterationStep) && isConverged) {
                break;
            }
            isLearned(iterationStep);
            currentLoss = totalLoss;
        }

        factorMatrix.iterateElement(MathCalculator.PARALLEL, (element) -> {
            int row = element.getRow();
            int column = element.getColumn();
            ArrayVector vector = featureVectors[column];
            float value = 0;
            for (VectorScalar entry : vector) {
                value += featureFactors.getValue(row, entry.getIndex()) * entry.getValue();
            }
            element.setValue(value);
        });
        featureVector = DenseVector.valueOf(numberOfItems);
        featureVector.iterateElement(MathCalculator.SERIAL, (element) -> {
            element.dotProduct(itemFeatures, featureVectors[element.getIndex()]).getValue();
        });
    }

    private float predict(int userIndex, int itemIndex, float itemFeature, MathVector factorVector) {
        DefaultScalar scalar = DefaultScalar.getInstance();
        scalar.setValue(0F);
        scalar.shiftValue(itemBiases.getValue(itemIndex) + itemFeature);
        scalar.accumulateProduct(userFactors.getRowVector(userIndex), itemFactors.getRowVector(itemIndex));
        scalar.accumulateProduct(userFeatures.getRowVector(userIndex), factorVector);
        return scalar.getValue();
    }

    @Override
    public float predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        return predict(userIndex, itemIndex, featureVector.getValue(itemIndex), factorMatrix.getColumnVector(itemIndex));
    }

}
