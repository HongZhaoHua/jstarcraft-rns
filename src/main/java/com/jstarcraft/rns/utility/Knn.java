package com.jstarcraft.rns.utility;

import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;

import com.jstarcraft.ai.math.structure.vector.ArrayVector;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.core.utility.Integer2FloatKeyValue;

import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2FloatSortedMap;

public class Knn {

    private int k;

    private Comparator<Integer2FloatKeyValue> comparator;

    private NavigableSet<Integer2FloatKeyValue> neighbors;

    private volatile Integer2FloatKeyValue keyValue;

    public Knn(int k, Comparator<Integer2FloatKeyValue> comparator) {
        this.k = k;
        this.comparator = comparator;
        this.neighbors = new TreeSet<>(comparator);
    }

    /**
     * 更新邻居
     * 
     * @param key
     * @param value
     */
    public synchronized void updateNeighbor(int key, float value) {
        Integer2FloatKeyValue keyValue = new Integer2FloatKeyValue(key, value);
        if (neighbors.size() >= k) {
            // 与边界值比较再判断是否更新
            if (comparator.compare(keyValue, this.keyValue) < 0) {
                neighbors.add(keyValue);
                neighbors.pollLast();
                this.keyValue = neighbors.last();
            }
        } else {
            neighbors.add(keyValue);
            this.keyValue = neighbors.last();
        }
    }

    /**
     * 获取邻居
     * 
     * @return
     */
    public MathVector getNeighbors() {
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

}
