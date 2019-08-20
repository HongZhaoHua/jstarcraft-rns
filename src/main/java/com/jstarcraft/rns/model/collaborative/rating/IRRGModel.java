package com.jstarcraft.rns.model.collaborative.rating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.HashMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.model.MatrixFactorizationModel;
import com.jstarcraft.rns.utility.LogisticUtility;

import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;

/**
 * 
 * IRRG推荐器
 * 
 * <pre>
 * Exploiting Implicit Item Relationships for Recommender Systems
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class IRRGModel extends MatrixFactorizationModel {

    /** item relationship regularization coefficient */
    private float correlationRegularization;

    /** adjust the reliability */
    // TODO 修改为配置.
    private float reliability = 50F;

    /** k nearest neighborhoods */
    // TODO 修改为配置.
    private int neighborSize = 50;

    /** store co-occurence between two items. */
    @Deprecated
    private Table<Integer, Integer, Integer> itemCount = HashBasedTable.create();

    /** store item-to-item AR */
    @Deprecated
    private Table<Integer, Integer, Float> itemCorrsAR = HashBasedTable.create();

    /** store sorted item-to-item AR */
    @Deprecated
    private Table<Integer, Integer, Float> itemCorrsAR_Sorted = HashBasedTable.create();

    /** store the complementary item-to-item AR */
    @Deprecated
    private Table<Integer, Integer, Float> itemCorrsAR_added = HashBasedTable.create();

    /** store group-to-item AR */
    @Deprecated
    private Map<Integer, List<KeyValue<KeyValue<Integer, Integer>, Float>>> itemCorrsGAR = new HashMap<>();

    private SparseMatrix complementMatrix;

    /** store sorted group-to-item AR */
    private Map<Integer, SparseMatrix> itemCorrsGAR_Sorted = new HashMap<>();

    // TODO 临时性表格,用于代替trainMatrix.getTermValue.
    @Deprecated
    Table<Integer, Integer, Float> dataTable = HashBasedTable.create();

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        for (MatrixScalar term : scoreMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            dataTable.put(userIndex, itemIndex, term.getValue());
        }

        correlationRegularization = configuration.getFloat("recommender.alpha");
        userFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(0.8F));
        });
        itemFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(0.8F));
        });

        computeAssociationRuleByItem();
        sortAssociationRuleByItem();
        computeAssociationRuleByGroup();
        sortAssociationRuleByGroup();
        complementAssociationRule();
        complementMatrix = SparseMatrix.valueOf(itemSize, itemSize, itemCorrsAR_added);
    }

    @Override
    protected void doPractice() {
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            totalError = 0F;

            DenseMatrix userDeltas = DenseMatrix.valueOf(userSize, factorSize);
            DenseMatrix itemDeltas = DenseMatrix.valueOf(itemSize, factorSize);
            for (MatrixScalar term : scoreMatrix) {
                int userIndex = term.getRow();
                int itemIndex = term.getColumn();
                float score = term.getValue();
                if (score <= 0F) {
                    continue;
                }
                float predict = super.predict(userIndex, itemIndex);
                float error = LogisticUtility.getValue(predict) - (score - minimumScore) / (maximumScore - minimumScore);
                float csgd = LogisticUtility.getGradient(predict) * error;

                totalError += error * error;
                for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                    float userFactor = userFactors.getValue(userIndex, factorIndex);
                    float itemFactor = itemFactors.getValue(itemIndex, factorIndex);
                    userDeltas.shiftValue(userIndex, factorIndex, csgd * itemFactor + userRegularization * userFactor);
                    itemDeltas.shiftValue(itemIndex, factorIndex, csgd * userFactor + itemRegularization * itemFactor);
                    totalError += userRegularization * userFactor * userFactor + itemRegularization * itemFactor * itemFactor;
                }
            }

            for (int leftItemIndex = 0; leftItemIndex < itemSize; leftItemIndex++) { // complementary
                // item-to-item
                // AR
                SparseVector itemVector = complementMatrix.getColumnVector(leftItemIndex);
                for (VectorScalar term : itemVector) {
                    int rightItemIndex = term.getIndex();
                    float skj = term.getValue();
                    for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                        float ekj = itemFactors.getValue(leftItemIndex, factorIndex) - itemFactors.getValue(rightItemIndex, factorIndex);
                        itemDeltas.shiftValue(leftItemIndex, factorIndex, correlationRegularization * skj * ekj);
                        totalError += correlationRegularization * skj * ekj * ekj;
                    }
                }
                itemVector = complementMatrix.getRowVector(leftItemIndex);
                for (VectorScalar term : itemVector) {
                    int rightItemIndex = term.getIndex();
                    float sjg = term.getValue();
                    for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                        float ejg = itemFactors.getValue(leftItemIndex, factorIndex) - itemFactors.getValue(rightItemIndex, factorIndex);
                        itemDeltas.shiftValue(leftItemIndex, factorIndex, correlationRegularization * sjg * ejg);
                    }
                }
            }

            // group-to-item AR
            for (Entry<Integer, SparseMatrix> leftKeyValue : itemCorrsGAR_Sorted.entrySet()) {
                int leftItemIndex = leftKeyValue.getKey();
                SparseMatrix leftTable = leftKeyValue.getValue();
                for (MatrixScalar term : leftTable) {
                    for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                        float egkj = (float) (itemFactors.getValue(leftItemIndex, factorIndex) - (itemFactors.getValue(term.getRow(), factorIndex) + itemFactors.getValue(term.getColumn(), factorIndex)) / Math.sqrt(2F));
                        float egkj_1 = correlationRegularization * term.getValue() * egkj;
                        itemDeltas.shiftValue(leftItemIndex, factorIndex, egkj_1);
                        totalError += egkj_1 * egkj;
                    }
                }
                for (Entry<Integer, SparseMatrix> rightKeyValue : itemCorrsGAR_Sorted.entrySet()) {
                    int rightItemIndex = rightKeyValue.getKey();
                    if (rightItemIndex != leftItemIndex) {
                        SparseMatrix rightTable = rightKeyValue.getValue();
                        SparseVector itemVector = rightTable.getRowVector(leftItemIndex);
                        for (VectorScalar term : itemVector) {
                            for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                                float ejgk = (float) (itemFactors.getValue(rightItemIndex, factorIndex) - (itemFactors.getValue(leftItemIndex, factorIndex) + itemFactors.getValue(term.getIndex(), factorIndex)) / Math.sqrt(2F));
                                float ejgk_1 = (float) (-correlationRegularization * term.getValue() * ejgk / Math.sqrt(2F));
                                itemDeltas.shiftValue(leftItemIndex, factorIndex, ejgk_1);
                            }
                        }
                    }
                }
            }

            userFactors.addMatrix(userDeltas.scaleValues(-learnRatio), false);
            itemFactors.addMatrix(itemDeltas.scaleValues(-learnRatio), false);

            totalError *= 0.5F;
            if (isConverged(epocheIndex) && isConverged) {
                break;
            }
            isLearned(epocheIndex);
            currentError = totalError;
        }
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        float score = super.predict(userIndex, itemIndex);
        score = LogisticUtility.getValue(score);
        score = minimumScore + score * (maximumScore - minimumScore);
        instance.setQuantityMark(score);
    }

    /**
     * 计算物品之间的关联规则
     */
    private void computeAssociationRuleByItem() {
        // TODO 此处可以参考Abstract.getScoreList的相似度计算.
        for (int leftItemIndex = 0; leftItemIndex < itemSize; leftItemIndex++) {
            if (scoreMatrix.getColumnScope(leftItemIndex) == 0) {
                continue;
            }
            SparseVector itemVector = scoreMatrix.getColumnVector(leftItemIndex);
            int total = itemVector.getElementSize();
            for (int rightItemIndex = 0; rightItemIndex < itemSize; rightItemIndex++) {
                if (leftItemIndex == rightItemIndex) {
                    continue;
                }
                float coefficient = 0F;
                int count = 0;
                for (VectorScalar term : itemVector) {
                    int userIndex = term.getIndex();
                    if (dataTable.contains(userIndex, rightItemIndex)) {
                        count++;
                    }
                }
                float shrink = count / (count + reliability);
                coefficient = shrink * count / total;
                if (coefficient > 0F) {
                    itemCorrsAR.put(leftItemIndex, rightItemIndex, coefficient);
                    itemCount.put(leftItemIndex, rightItemIndex, count);
                }
            }
        }
    }

    /**
     * 排序关联规则
     */
    private void sortAssociationRuleByItem() {
        for (int leftItemIndex : itemCorrsAR.columnKeySet()) {
            int size = itemCorrsAR.column(leftItemIndex).size();
            float temp[][] = new float[size][3];
            int flag = 0;
            for (int rightItemIndex : itemCorrsAR.column(leftItemIndex).keySet()) {
                temp[flag][0] = rightItemIndex;
                temp[flag][1] = leftItemIndex;
                temp[flag][2] = itemCorrsAR.get(rightItemIndex, leftItemIndex);
                flag++;
            }
            if (size > neighborSize) {
                for (int i = 0; i < neighborSize; i++) { // sort k nearest
                                                         // neighbors
                    for (int j = i + 1; j < size; j++) {
                        if (temp[i][2] < temp[j][2]) {
                            for (int k = 0; k < 3; k++) {
                                float trans = temp[i][k];
                                temp[i][k] = temp[j][k];
                                temp[j][k] = trans;
                            }
                        }
                    }
                }
                storeAssociationRule(neighborSize, temp);
            } else {
                storeAssociationRule(size, temp);
            }
        }
    }

    /**
     * 保存关联规则
     * 
     * @param size
     * @param temp
     */
    private void storeAssociationRule(int size, float temp[][]) {
        for (int i = 0; i < size; i++) {
            int leftItemIndex = (int) (temp[i][0]);
            int rightItemIndex = (int) (temp[i][1]);
            itemCorrsAR_Sorted.put(leftItemIndex, rightItemIndex, temp[i][2]);
        }
    }

    /**
     * Find out itemsets which contain three items and store them into mylist.
     */
    private void computeAssociationRuleByGroup() {
        for (int groupIndex : itemCorrsAR.columnKeySet()) {
            Integer[] itemIndexes = itemCorrsAR_Sorted.column(groupIndex).keySet().toArray(new Integer[] {});
            LinkedList<KeyValue<Integer, Integer>> groupItemList = new LinkedList<>();
            for (int leftIndex = 0; leftIndex < itemIndexes.length - 1; leftIndex++) {
                for (int rightIndex = leftIndex + 1; rightIndex < itemIndexes.length; rightIndex++) {
                    if (itemCount.contains(itemIndexes[leftIndex], itemIndexes[rightIndex])) {
                        groupItemList.add(new KeyValue<>(itemIndexes[leftIndex], itemIndexes[rightIndex]));
                    }
                }
            }
            computeAssociationRuleByGroup(groupIndex, groupItemList);
        }
    }

    /**
     * Compute group-to-item AR and store them into map itemCorrsGAR
     */
    private void computeAssociationRuleByGroup(int groupIndex, LinkedList<KeyValue<Integer, Integer>> itemList) {
        List<KeyValue<KeyValue<Integer, Integer>, Float>> coefficientList = new LinkedList<>();

        for (KeyValue<Integer, Integer> keyValue : itemList) {
            int leftIndex = keyValue.getKey();
            int rightIndex = keyValue.getValue();
            SparseVector groupVector = scoreMatrix.getColumnVector(groupIndex);
            int count = 0;
            for (VectorScalar term : groupVector) {
                int userIndex = term.getIndex();
                if (dataTable.contains(userIndex, leftIndex) && dataTable.contains(userIndex, rightIndex)) {
                    count++;
                }
            }
            if (count > 0) {
                float shrink = count / (count + reliability);
                int co_bc = itemCount.get(leftIndex, rightIndex);
                float coefficient = shrink * (count + 0F) / co_bc;
                coefficientList.add(new KeyValue<>(keyValue, coefficient));
            }
        }
        itemCorrsGAR.put(groupIndex, new ArrayList<>(coefficientList));
    }

    /**
     * Order group-to-item AR and store them into map itemCorrsGAR_Sorted
     */
    private void sortAssociationRuleByGroup() {
        for (int groupIndex : itemCorrsGAR.keySet()) {
            List<KeyValue<KeyValue<Integer, Integer>, Float>> list = itemCorrsGAR.get(groupIndex);
            if (list.size() > neighborSize) {
                Collections.sort(list, (left, right) -> {
                    return right.getValue().compareTo(left.getValue());
                });
                list = list.subList(0, neighborSize);
            }

            HashMatrix groupTable = new HashMatrix(true, itemSize, itemSize, new Long2FloatRBTreeMap());
            for (KeyValue<KeyValue<Integer, Integer>, Float> keyValue : list) {
                int leftItemIndex = keyValue.getKey().getKey();
                int rightItemIndex = keyValue.getKey().getValue();
                float correlation = keyValue.getValue();
                groupTable.setValue(leftItemIndex, rightItemIndex, correlation);
            }
            itemCorrsGAR_Sorted.put(groupIndex, SparseMatrix.valueOf(itemSize, itemSize, groupTable));
        }
    }

    /**
     * Select item-to-item AR to complement group-to-item AR
     */
    /**
     * 选择物品关联规则补充分组关联规则.
     */
    private void complementAssociationRule() {
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            if (scoreMatrix.getColumnScope(itemIndex) == 0) {
                continue;
            }
            SparseMatrix groupTable = itemCorrsGAR_Sorted.get(itemIndex);
            if (groupTable != null) {
                int groupSize = groupTable.getElementSize();
                if (groupSize < neighborSize) {
                    int complementSize = neighborSize - groupSize;
                    int itemSize = itemCorrsAR_Sorted.column(itemIndex).size();
                    // TODO 使用KeyValue代替.
                    float[][] trans = new float[itemSize][2];
                    if (itemSize > complementSize) {
                        int count = 0;
                        for (int id : itemCorrsAR_Sorted.column(itemIndex).keySet()) {
                            float value = itemCorrsAR_Sorted.get(id, itemIndex);
                            trans[count][0] = id;
                            trans[count][1] = value;
                            count++;
                        }
                        for (int x = 0; x < complementSize; x++) {
                            for (int y = x + 1; y < trans.length; y++) {
                                float x_value = trans[x][1];
                                float y_value = trans[y][1];
                                if (x_value < y_value) {
                                    for (int z = 0; z < 2; z++) {
                                        float tran = trans[x][z];
                                        trans[x][z] = trans[y][z];
                                        trans[y][z] = tran;
                                    }
                                }
                            }
                        }
                        for (int x = 0; x < complementSize; x++) {
                            int id = (int) (trans[x][0]);
                            float value = trans[x][1];
                            itemCorrsAR_added.put(id, itemIndex, value);
                        }
                    } else {
                        storeCAR(itemIndex);
                    }
                }
            } else {
                storeCAR(itemIndex);
            }
        }
    }

    /**
     * Function to store complementary item-to-item AR into table itemCorrsAR_added.
     *
     * @param leftItemIndex
     */
    private void storeCAR(int leftItemIndex) {
        for (int rightItemIndex : itemCorrsAR_Sorted.column(leftItemIndex).keySet()) {
            float value = itemCorrsAR_Sorted.get(rightItemIndex, leftItemIndex);
            itemCorrsAR_added.put(rightItemIndex, leftItemIndex, value);
        }
    }

}
