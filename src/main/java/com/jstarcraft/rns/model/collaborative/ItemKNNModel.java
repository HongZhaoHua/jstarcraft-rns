package com.jstarcraft.rns.model.collaborative;

import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.algorithm.correlation.Correlation;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.matrix.SymmetryMatrix;
import com.jstarcraft.ai.math.structure.vector.ArrayVector;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.core.common.reflection.ReflectionUtility;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.core.utility.Integer2FloatKeyValue;
import com.jstarcraft.rns.model.AbstractModel;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

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

    protected SymmetryMatrix symmetryMatrix;

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
            int value = -(Float.compare(left.getValue(), right.getValue()));
            if (value == 0) {
                value = Integer.compare(left.getKey(), right.getKey());
            }
            return value;
        }

    };

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        neighborSize = configuration.getInteger("recommender.neighbors.knn.number", 50);
        // TODO 修改为配置枚举
        try {
            Class<Correlation> correlationClass = (Class<Correlation>) Class.forName(configuration.getString("recommender.correlation.class"));
            Correlation correlation = ReflectionUtility.getInstance(correlationClass);
            symmetryMatrix = new SymmetryMatrix(scoreMatrix.getColumnSize());
            correlation.calculateCoefficients(scoreMatrix, true, symmetryMatrix::setValue);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        itemMeans = DenseVector.valueOf(itemSize);

        // TODO 设置容量
        itemNeighbors = new MathVector[itemSize];
        Int2ObjectMap<TreeSet<Integer2FloatKeyValue>> itemNNs = new Int2ObjectOpenHashMap<>();
        for (MatrixScalar term : symmetryMatrix) {
            int row = term.getRow();
            int column = term.getColumn();
            float value = term.getValue();
            if (row == column) {
                continue;
            }
            // 忽略相似度为0的物品
            if (value == 0F) {
                continue;
            }
            TreeSet<Integer2FloatKeyValue> neighbors = itemNNs.get(row);
            if (neighbors == null) {
                neighbors = new TreeSet<>(comparator);
                itemNNs.put(row, neighbors);
            }
            neighbors.add(new Integer2FloatKeyValue(column, value));
            neighbors = itemNNs.get(column);
            if (neighbors == null) {
                neighbors = new TreeSet<>(comparator);
                itemNNs.put(column, neighbors);
            }
            neighbors.add(new Integer2FloatKeyValue(row, value));
        }

        // 构建物品邻居映射
        for (Int2ObjectMap.Entry<TreeSet<Integer2FloatKeyValue>> term : itemNNs.int2ObjectEntrySet()) {
            TreeSet<Integer2FloatKeyValue> neighbors = term.getValue();
            int size = neighbors.size() < neighborSize ? neighbors.size() : neighborSize;
            int[] indexes = new int[size];
            float[] values = new float[size];
            int index = 0;
            for (Integer2FloatKeyValue neighbor : neighbors) {
                indexes[index++] = neighbor.getKey();
                if (index >= neighborSize) {
                    break;
                }
            }
            Arrays.sort(indexes);
            for (int cursor = 0; cursor < size; cursor++) {
                values[cursor] = symmetryMatrix.getValue(term.getIntKey(), indexes[cursor]);
            }
            itemNeighbors[term.getIntKey()] = new ArrayVector(size, indexes, values);
        }

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
