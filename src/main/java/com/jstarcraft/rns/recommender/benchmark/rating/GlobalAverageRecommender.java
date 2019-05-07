package com.jstarcraft.rns.recommender.benchmark.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.modem.ModemDefinition;
import com.jstarcraft.rns.recommender.AbstractRecommender;

/**
 * 
 * Global Average推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
@ModemDefinition(value = { "meanOfScore" })
public class GlobalAverageRecommender extends AbstractRecommender {

    @Override
    protected void doPractice() {
    }

    @Override
    public float predict(DataInstance instance) {
        return meanOfScore;
    }

}
