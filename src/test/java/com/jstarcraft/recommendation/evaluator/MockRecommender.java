package com.jstarcraft.recommendation.evaluator;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.recommendation.configurator.Configuration;
import com.jstarcraft.recommendation.recommender.Recommender;

public class MockRecommender implements Recommender {

    private int itemDimension;

    private SparseMatrix matrix;

    MockRecommender(int itemDimension, SparseMatrix matrix) {
        this.itemDimension = itemDimension;
        this.matrix = matrix;
    }

    @Override
    public void prepare(Configuration configuration, DataModule model, DataSpace space) {
    }

    @Override
    public void practice() {
    }

    @Override
    public float predict(DataInstance instance) {
        return matrix.getColumnScope(instance.getQualityFeature(itemDimension));
    }

}
