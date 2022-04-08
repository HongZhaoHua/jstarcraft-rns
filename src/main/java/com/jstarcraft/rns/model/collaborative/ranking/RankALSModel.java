package com.jstarcraft.rns.model.collaborative.ranking;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.MatrixUtility;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.common.configuration.Option;
import com.jstarcraft.rns.model.MatrixFactorizationModel;

/**
 * 
 * Rank ALS推荐器
 * 
 * <pre>
 * Alternating Least Squares for Personalized Ranking
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class RankALSModel extends MatrixFactorizationModel {
    // whether support based weighting is used ($s_i=|U_i|$) or not ($s_i=1$)
    private boolean weight;

    private DenseVector weightVector;

    private float sumSupport;

    // TODO 考虑重构到父类
    private List<Integer> userList;

    // TODO 考虑重构到父类
    private List<Integer> itemList;

    @Override
    public void prepare(Option configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        weight = configuration.getBoolean("recommender.rankals.support.weight", true);
        weightVector = DenseVector.valueOf(itemSize);
        sumSupport = 0;
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            float supportValue = weight ? scoreMatrix.getColumnScope(itemIndex) : 1F;
            weightVector.setValue(itemIndex, supportValue);
            sumSupport += supportValue;
        }

        userList = new LinkedList<>();
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            if (scoreMatrix.getRowVector(userIndex).getElementSize() > 0) {
                userList.add(userIndex);
            }
        }
        userList = new ArrayList<>(userList);

        itemList = new LinkedList<>();
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            if (scoreMatrix.getColumnVector(itemIndex).getElementSize() > 0) {
                itemList.add(itemIndex);
            }
        }
        itemList = new ArrayList<>(itemList);
    }

    @Override
    protected void doPractice() {
        // 缓存特征计算,避免消耗内存
        DenseMatrix matrixCache = DenseMatrix.valueOf(factorSize, factorSize);
        DenseMatrix copyCache = DenseMatrix.valueOf(factorSize, factorSize);
        DenseVector vectorCache = DenseVector.valueOf(factorSize);
        DenseMatrix inverseCache = DenseMatrix.valueOf(factorSize, factorSize);

        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            // P step: update user vectors
            // 特征权重矩阵和特征权重向量
            DenseMatrix factorWeightMatrix = DenseMatrix.valueOf(factorSize, factorSize);
            DenseVector factorWeightVector = DenseVector.valueOf(factorSize);
            for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                float weight = weightVector.getValue(itemIndex);
                DenseVector itemVector = itemFactors.getRowVector(itemIndex);
                factorWeightMatrix.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                    int row = scalar.getRow();
                    int column = scalar.getColumn();
                    float value = scalar.getValue();
                    scalar.setValue(value + (itemVector.getValue(row) * itemVector.getValue(column) * weight));
                });
                factorWeightVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                    int index = scalar.getIndex();
                    float value = scalar.getValue();
                    scalar.setValue(value + itemVector.getValue(index) * weight);
                });
            }

            // 用户特征矩阵,用户权重向量,用户评分向量,用户次数向量.
            DenseMatrix userDeltas = DenseMatrix.valueOf(userSize, factorSize);
            DenseVector userWeights = DenseVector.valueOf(userSize);
            DenseVector userScores = DenseVector.valueOf(userSize);
            DenseVector userTimes = DenseVector.valueOf(userSize);
            // 根据物品特征构建用户特征
            for (int userIndex : userList) {
                // for each user
                SparseVector userVector = scoreMatrix.getRowVector(userIndex);

                // TODO 此处考虑重构,尽量减少数组构建
                DenseMatrix factorValues = DenseMatrix.valueOf(factorSize, factorSize);
                DenseMatrix copyValues = DenseMatrix.valueOf(factorSize, factorSize);
                DenseVector rateValues = DenseVector.valueOf(factorSize);
                DenseVector weightValues = DenseVector.valueOf(factorSize);
                float weightSum = 0F, rateSum = 0F, timeSum = userVector.getElementSize();
                for (VectorScalar term : userVector) {
                    int itemIndex = term.getIndex();
                    float score = term.getValue();
                    // double cui = 1;
                    DenseVector itemVector = itemFactors.getRowVector(itemIndex);
                    factorValues.iterateElement(MathCalculator.PARALLEL, (scalar) -> {
                        int row = scalar.getRow();
                        int column = scalar.getColumn();
                        float value = scalar.getValue();
                        scalar.setValue(value + itemVector.getValue(row) * itemVector.getValue(column));
                    });
                    // ratings of unrated items will be 0
                    float weight = weightVector.getValue(itemIndex) * score;
                    float value;
                    for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                        value = itemVector.getValue(factorIndex);
                        userDeltas.shiftValue(userIndex, factorIndex, value);
                        rateValues.shiftValue(factorIndex, value * score);
                        weightValues.shiftValue(factorIndex, value * weight);
                    }

                    rateSum += score;
                    weightSum += weight;
                }

                factorValues.iterateElement(MathCalculator.PARALLEL, (scalar) -> {
                    int row = scalar.getRow();
                    int column = scalar.getColumn();
                    float value = scalar.getValue();
                    scalar.setValue((row == column ? userRegularization : 0F) + value * sumSupport - (userDeltas.getValue(userIndex, row) * factorWeightVector.getValue(column)) - (factorWeightVector.getValue(row) * userDeltas.getValue(userIndex, column)) + (factorWeightMatrix.getValue(row, column) * timeSum));
                });
                float rateScale = rateSum;
                float weightScale = weightSum;
                rateValues.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                    int index = scalar.getIndex();
                    float value = scalar.getValue();
                    scalar.setValue((value * sumSupport - userDeltas.getValue(userIndex, index) * weightScale) - (factorWeightVector.getValue(index) * rateScale) + (weightValues.getValue(index) * timeSum));
                });
                userFactors.getRowVector(userIndex).dotProduct(MatrixUtility.inverse(factorValues, copyValues, inverseCache), false, rateValues, MathCalculator.SERIAL);

                userWeights.setValue(userIndex, weightSum);
                userScores.setValue(userIndex, rateSum);
                userTimes.setValue(userIndex, timeSum);
            }

            // Q step: update item vectors
            DenseMatrix itemFactorMatrix = DenseMatrix.valueOf(factorSize, factorSize);
            DenseMatrix itemTimeMatrix = DenseMatrix.valueOf(factorSize, factorSize);
            DenseVector itemFactorVector = DenseVector.valueOf(factorSize);
            DenseVector factorValues = DenseVector.valueOf(factorSize);
            for (int userIndex : userList) {
                DenseVector userVector = userFactors.getRowVector(userIndex);
                matrixCache.dotProduct(userVector, userVector, MathCalculator.SERIAL);
                itemFactorMatrix.addMatrix(matrixCache, false);
                itemTimeMatrix.iterateElement(MathCalculator.PARALLEL, (scalar) -> {
                    int row = scalar.getRow();
                    int column = scalar.getColumn();
                    float value = scalar.getValue();
                    scalar.setValue(value + (matrixCache.getValue(row, column) * userTimes.getValue(userIndex)));
                });
                itemFactorVector.addVector(vectorCache.dotProduct(matrixCache, false, userDeltas.getRowVector(userIndex), MathCalculator.SERIAL));
                float rateSum = userScores.getValue(userIndex);
                factorValues.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                    int index = scalar.getIndex();
                    float value = scalar.getValue();
                    scalar.setValue(value + userVector.getValue(index) * rateSum);
                });
            }

            // 根据用户特征构建物品特征
            for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                // for each item
                SparseVector itemVector = scoreMatrix.getColumnVector(itemIndex);

                // TODO 此处考虑重构,尽量减少数组构建
                DenseVector rateValues = DenseVector.valueOf(factorSize);
                DenseVector weightValues = DenseVector.valueOf(factorSize);
                DenseVector timeValues = DenseVector.valueOf(factorSize);
                for (VectorScalar term : itemVector) {
                    int userIndex = term.getIndex();
                    float score = term.getValue();
                    float weight = userWeights.getValue(userIndex);
                    float time = score * userTimes.getValue(userIndex);
                    float value;
                    DenseVector userVector = userFactors.getRowVector(userIndex);
                    for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                        value = userVector.getValue(factorIndex);
                        rateValues.shiftValue(factorIndex, value * score);
                        weightValues.shiftValue(factorIndex, value * weight);
                        timeValues.shiftValue(factorIndex, value * time);
                    }
                }

                float weight = weightVector.getValue(itemIndex);
                vectorCache.dotProduct(itemFactorMatrix, false, factorWeightVector, MathCalculator.SERIAL);
                matrixCache.iterateElement(MathCalculator.PARALLEL, (scalar) -> {
                    int row = scalar.getRow();
                    int column = scalar.getColumn();
                    scalar.setValue(itemFactorMatrix.getValue(row, column) * (weight + 1));
                });
                DenseVector itemValues = itemFactors.getRowVector(itemIndex);
                vectorCache.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                    int index = scalar.getIndex();
                    float value = scalar.getValue();
                    value = value + (rateValues.getValue(index) * sumSupport) - weightValues.getValue(index) + (itemFactorVector.getValue(index) * weight) - (factorValues.getValue(index) * weight) + (timeValues.getValue(index) * weight);
                    value = value - scalar.dotProduct(matrixCache.getRowVector(index), itemValues).getValue();
                    scalar.setValue(value);
                });
                matrixCache.iterateElement(MathCalculator.PARALLEL, (scalar) -> {
                    int row = scalar.getRow();
                    int column = scalar.getColumn();
                    float value = scalar.getValue();
                    scalar.setValue((row == column ? itemRegularization : 0F) + (value / (weight + 1)) * sumSupport + itemTimeMatrix.getValue(row, column) * weight - value);
                });
                itemValues.dotProduct(MatrixUtility.inverse(matrixCache, copyCache, inverseCache), false, vectorCache, MathCalculator.SERIAL);
            }
            if (isConverged(epocheIndex) && isConverged) {
                break;
            }
            currentError = totalError;
        }
    }

}
