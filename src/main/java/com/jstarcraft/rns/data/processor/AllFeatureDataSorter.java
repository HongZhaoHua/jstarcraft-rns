package com.jstarcraft.rns.data.processor;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.processor.DataSorter;

public class AllFeatureDataSorter implements DataSorter {

    @Override
    public int sort(DataInstance left, DataInstance right) {
        for (int dimension = 0, order = left.getQualityOrder(); dimension < order; dimension++) {
            int leftValue = left.getQualityFeature(dimension);
            int rightValue = right.getQualityFeature(dimension);
            if (leftValue != rightValue) {
                return leftValue < rightValue ? -1 : 1;
            }
        }
        for (int dimension = 0, order = right.getQuantityOrder(); dimension < order; dimension++) {
            float leftValue = left.getQuantityFeature(dimension);
            float rightValue = right.getQuantityFeature(dimension);
            if (leftValue != rightValue) {
                return leftValue < rightValue ? -1 : 1;
            }
        }
        return 0;
    }

}
