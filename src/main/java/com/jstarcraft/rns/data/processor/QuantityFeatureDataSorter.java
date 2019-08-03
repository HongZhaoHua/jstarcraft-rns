package com.jstarcraft.rns.data.processor;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.processor.DataSorter;

public class QuantityFeatureDataSorter implements DataSorter {

    private int dimension;

    public QuantityFeatureDataSorter(int dimension) {
        this.dimension = dimension;
    }

    @Override
    public int sort(DataInstance left, DataInstance right) {
        float leftValue = left.getQuantityFeature(dimension);
        float rightValue = right.getQuantityFeature(dimension);
        if (leftValue != rightValue) {
            return leftValue < rightValue ? -1 : 1;
        }
        return 0;
    }

}
