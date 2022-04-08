package com.jstarcraft.rns.model.collaborative;

import java.util.Collection;
import java.util.Comparator;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.algorithm.correlation.MathCorrelation;
import com.jstarcraft.ai.math.structure.vector.ArrayVector;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.core.common.configuration.Option;
import com.jstarcraft.core.common.reflection.ReflectionUtility;
import com.jstarcraft.core.utility.Integer2FloatKeyValue;
import com.jstarcraft.core.utility.Neighborhood;
import com.jstarcraft.rns.model.AbstractModel;

import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2FloatSortedMap;

/**
 * 
 * Item KNN推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public abstract class ItemKNNModel extends AbstractModel {

    /** 邻居数量 */
    private int neighborSize;

    protected DenseVector itemMeans;

    /**
     * item's nearest neighbors for kNN > 0
     */
    protected MathVector[] itemNeighbors;

    protected SparseVector[] userVectors;

    protected SparseVector[] itemVectors;

    private Comparator<Integer2FloatKeyValue> comparator = new Comparator<Integer2FloatKeyValue>() {

        @Override
        public int compare(Integer2FloatKeyValue left, Integer2FloatKeyValue right) {
            int compare = -(Float.compare(left.getValue(), right.getValue()));
            if (compare == 0) {
                compare = Integer.compare(left.getKey(), right.getKey());
            }
            return compare;
        }

    };

    protected MathVector getNeighborVector(Collection<Integer2FloatKeyValue> neighbors) {
        int size = neighbors.size();
        int[] indexes = new int[size];
        float[] values = new float[size];
        Int2FloatSortedMap keyValues = new Int2FloatRBTreeMap();
        for (Integer2FloatKeyValue term : neighbors) {
            keyValues.put(term.getKey(), term.getValue());
        }
        int cursor = 0;
        for (Int2FloatMap.Entry term : keyValues.int2FloatEntrySet()) {
            indexes[cursor] = term.getIntKey();
            values[cursor] = term.getFloatValue();
            cursor++;
        }
        return new ArrayVector(size, indexes, values);
    }

    @Override
    public void prepare(Option configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        neighborSize = configuration.getInteger("recommender.neighbors.knn.number", 50);
        // TODO 设置容量
        itemNeighbors = new MathVector[itemSize];
        Neighborhood<Integer2FloatKeyValue>[] knns = new Neighborhood[itemSize];
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            knns[itemIndex] = new Neighborhood<>(neighborSize, comparator);
        }
        // TODO 修改为配置枚举
        try {
            Class<MathCorrelation> correlationClass = (Class<MathCorrelation>) Class.forName(configuration.getString("recommender.correlation.class"));
            MathCorrelation correlation = ReflectionUtility.getInstance(correlationClass);
            correlation.calculateCoefficients(scoreMatrix, true, (leftIndex, rightIndex, coefficient) -> {
                if (leftIndex == rightIndex) {
                    return;
                }
                // 忽略相似度为0的物品
                if (coefficient == 0F) {
                    return;
                }
                knns[leftIndex].updateNeighbor(new Integer2FloatKeyValue(rightIndex, coefficient));
                knns[rightIndex].updateNeighbor(new Integer2FloatKeyValue(leftIndex, coefficient));
            });
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            itemNeighbors[itemIndex] = getNeighborVector(knns[itemIndex].getNeighbors());
        }

        itemMeans = DenseVector.valueOf(itemSize);

        userVectors = new SparseVector[userSize];
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            userVectors[userIndex] = scoreMatrix.getRowVector(userIndex);
        }

        itemVectors = new SparseVector[itemSize];
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            itemVectors[itemIndex] = scoreMatrix.getColumnVector(itemIndex);
        }
    }

    @Override
    protected void doPractice() {
        meanScore = scoreMatrix.getSum(false) / scoreMatrix.getElementSize();
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            SparseVector itemVector = scoreMatrix.getColumnVector(itemIndex);
            itemMeans.setValue(itemIndex, itemVector.getElementSize() > 0 ? itemVector.getSum(false) / itemVector.getElementSize() : meanScore);
        }
    }

}
