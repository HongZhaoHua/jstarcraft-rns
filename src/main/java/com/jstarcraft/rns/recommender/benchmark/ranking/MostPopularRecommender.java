package com.jstarcraft.rns.recommender.benchmark.ranking;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.modem.ModemDefinition;
import com.jstarcraft.rns.configurator.Configuration;
import com.jstarcraft.rns.recommender.AbstractRecommender;

/**
 * 
 * Most Popular推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
@ModemDefinition(value = { "itemDimension", "populars" })
public class MostPopularRecommender extends AbstractRecommender {

    private int[] populars;

    @Override
    public void prepare(Configuration configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        populars = new int[numberOfItems];
    }

    @Override
    protected void doPractice() {
        for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
            populars[itemIndex] = scoreMatrix.getColumnScope(itemIndex);
        }
    }

    @Override
    public float predict(DataInstance instance) {
        int itemIndex = instance.getQualityFeature(itemDimension);
        return populars[itemIndex];
    }

}
