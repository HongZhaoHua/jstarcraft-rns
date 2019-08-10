package com.jstarcraft.rns.model.collaborative.ranking;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.model.MatrixFactorizationModel;

/**
 * 
 * FISM-AUC推荐器
 * 
 * <pre>
 * FISM: Factored Item Similarity Models for Top-N Recommender Systems
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
// 注意:FISM使用itemFactors来组成userFactors
public class FISMAUCModel extends MatrixFactorizationModel {

    private float rho, alpha, beta, gamma;

    /**
     * bias regularization
     */
    private float biasRegularization;

    /**
     * items and users biases vector
     */
    private DenseVector itemBiases;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        // 注意:FISM使用itemFactors来组成userFactors
        userFactors = DenseMatrix.valueOf(itemSize, factorSize);
        userFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });
        itemFactors = DenseMatrix.valueOf(itemSize, factorSize);
        itemFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });
        // TODO
        itemBiases = DenseVector.valueOf(itemSize);
        itemBiases.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });
        rho = configuration.getFloat("recommender.fismauc.rho");// 3-15
        alpha = configuration.getFloat("recommender.fismauc.alpha", 0.5F);
        beta = configuration.getFloat("recommender.fismauc.beta", 0.6F);
        gamma = configuration.getFloat("recommender.fismauc.gamma", 0.1F);
        biasRegularization = configuration.getFloat("recommender.iteration.learnrate", 0.0001F);
        // cacheSpec = conf.get("guava.cache.spec",
        // "maximumSize=200,expireAfterAccess=2m");
    }

    @Override
    protected void doPractice() {
        DefaultScalar scalar = DefaultScalar.getInstance();
        // x <- 0
        DenseVector userVector = DenseVector.valueOf(factorSize);
        // t <- (n - 1)^(-alpha) Σ pj (j!=i)
        DenseVector itemVector = DenseVector.valueOf(factorSize);

        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            totalError = 0F;
            // for all u in C
            for (int userIndex = 0; userIndex < userSize; userIndex++) {
                SparseVector rateVector = scoreMatrix.getRowVector(userIndex);
                int size = rateVector.getElementSize();
                if (size == 0 || size == 1) {
                    size = 2;
                }
                // for all i in Ru+
                for (VectorScalar positiveTerm : rateVector) {
                    int positiveIndex = positiveTerm.getIndex();
                    userVector.setValues(0F);
                    itemVector.setValues(0F);
                    for (VectorScalar negativeTerm : rateVector) {
                        int negativeIndex = negativeTerm.getIndex();
                        if (positiveIndex != negativeIndex) {
                            itemVector.addVector(userFactors.getRowVector(negativeIndex));
                        }
                    }
                    itemVector.scaleValues((float) Math.pow(size - 1, -alpha));
                    // Z <- SampleZeros(rho)
                    int sampleSize = (int) (rho * size);
                    // make a random sample of negative feedback for Ru-
                    List<Integer> negativeIndexes = new LinkedList<>();
                    for (int sampleIndex = 0; sampleIndex < sampleSize; sampleIndex++) {
                        int negativeItemIndex = RandomUtility.randomInteger(itemSize - negativeIndexes.size());
                        int index = 0;
                        for (int negativeIndex : negativeIndexes) {
                            if (negativeItemIndex >= negativeIndex) {
                                negativeItemIndex++;
                                index++;
                            } else {
                                break;
                            }
                        }
                        negativeIndexes.add(index, negativeItemIndex);
                    }

                    int leftIndex = 0, rightIndex = 0, leftSize = rateVector.getElementSize(), rightSize = sampleSize;
                    if (leftSize != 0 && rightSize != 0) {
                        Iterator<VectorScalar> leftIterator = rateVector.iterator();
                        Iterator<Integer> rightIterator = negativeIndexes.iterator();
                        VectorScalar leftTerm = leftIterator.next();
                        int negativeItemIndex = rightIterator.next();
                        // 判断两个有序数组中是否存在相同的数字
                        while (leftIndex < leftSize && rightIndex < rightSize) {
                            if (leftTerm.getIndex() == negativeItemIndex) {
                                if (leftIterator.hasNext()) {
                                    leftTerm = leftIterator.next();
                                }
                                rightIterator.remove();
                                if (rightIterator.hasNext()) {
                                    negativeItemIndex = rightIterator.next();
                                }
                                leftIndex++;
                                rightIndex++;
                            } else if (leftTerm.getIndex() > negativeItemIndex) {
                                if (rightIterator.hasNext()) {
                                    negativeItemIndex = rightIterator.next();
                                }
                                rightIndex++;
                            } else if (leftTerm.getIndex() < negativeItemIndex) {
                                if (leftIterator.hasNext()) {
                                    leftTerm = leftIterator.next();
                                }
                                leftIndex++;
                            }
                        }
                    }

                    // for all j in Z
                    for (int negativeIndex : negativeIndexes) {
                        // update pui puj rui ruj
                        float positiveScore = positiveTerm.getValue();
                        float negativeScore = 0F;
                        float positiveBias = itemBiases.getValue(positiveIndex);
                        float negativeBias = itemBiases.getValue(negativeIndex);
                        float positiveFactor = positiveBias + scalar.dotProduct(itemFactors.getRowVector(positiveIndex), itemVector).getValue();
                        float negativeFactor = negativeBias + scalar.dotProduct(itemFactors.getRowVector(negativeIndex), itemVector).getValue();

                        float error = (positiveScore - negativeScore) - (positiveFactor - negativeFactor);
                        totalError += error * error;

                        // update bi bj
                        itemBiases.shiftValue(positiveIndex, biasRegularization * (error - gamma * positiveBias));
                        itemBiases.shiftValue(negativeIndex, biasRegularization * (error - gamma * negativeBias));

                        // update qi qj
                        DenseVector positiveVector = itemFactors.getRowVector(positiveIndex);
                        positiveVector.iterateElement(MathCalculator.SERIAL, (element) -> {
                            int index = element.getIndex();
                            float value = element.getValue();
                            element.setValue(value + (itemVector.getValue(index) * error - value * beta) * biasRegularization);
                        });
                        DenseVector negativeVector = itemFactors.getRowVector(negativeIndex);
                        negativeVector.iterateElement(MathCalculator.SERIAL, (element) -> {
                            int index = element.getIndex();
                            float value = element.getValue();
                            element.setValue(value - (itemVector.getValue(index) * error - value * beta) * biasRegularization);
                        });

                        // update x
                        userVector.iterateElement(MathCalculator.SERIAL, (element) -> {
                            int index = element.getIndex();
                            float value = element.getValue();
                            element.setValue(value + (positiveVector.getValue(index) - negativeVector.getValue(index)) * error);
                        });
                    }

                    float scale = (float) (Math.pow(rho, -1) * Math.pow(size - 1, -alpha));

                    // for all j in Ru+\{i}
                    for (VectorScalar term : rateVector) {
                        int negativeIndex = term.getIndex();
                        if (negativeIndex != positiveIndex) {
                            // update pj
                            DenseVector negativeVector = userFactors.getRowVector(negativeIndex);
                            negativeVector.iterateElement(MathCalculator.SERIAL, (element) -> {
                                int index = element.getIndex();
                                float value = element.getValue();
                                element.setValue((userVector.getValue(index) * scale - value * beta) * biasRegularization + value);
                            });
                        }
                    }
                }
            }
            for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                double itemBias = itemBiases.getValue(itemIndex);
                totalError += gamma * itemBias * itemBias;
                totalError += beta * scalar.dotProduct(itemFactors.getRowVector(itemIndex), itemFactors.getRowVector(itemIndex)).getValue();
                totalError += beta * scalar.dotProduct(userFactors.getRowVector(itemIndex), userFactors.getRowVector(itemIndex)).getValue();
            }

            totalError *= 0.5F;
            if (isConverged(epocheIndex) && isConverged) {
                break;
            }
            currentError = totalError;
        }
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        DefaultScalar scalar = DefaultScalar.getInstance();
        float bias = itemBiases.getValue(itemIndex);
        float sum = 0F;
        int count = 0;
        for (VectorScalar term : scoreMatrix.getRowVector(userIndex)) {
            int compareIndex = term.getIndex();
            // for test, i and j will be always unequal as j is unrated
            if (compareIndex != itemIndex) {
                DenseVector compareVector = userFactors.getRowVector(compareIndex);
                DenseVector itemVector = itemFactors.getRowVector(itemIndex);
                sum += scalar.dotProduct(compareVector, itemVector).getValue();
                count++;
            }
        }
        sum *= (float) (count > 0 ? Math.pow(count, -alpha) : 0F);
        instance.setQuantityMark(bias + sum);
    }

}
