package com.jstarcraft.rns.data.processor;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.processor.DataSplitter;

public class QualityFeatureDataSplitter implements DataSplitter {

    private int dimension;

    public QualityFeatureDataSplitter(int dimension) {
        this.dimension = dimension;
    }
    
    @Override
    public int split(DataInstance instance) {
        return instance.getQualityFeature(dimension);
    }

}
