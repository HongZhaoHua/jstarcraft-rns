package com.jstarcraft.rns.recommend.benchmark.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.modem.ModemDefinition;
import com.jstarcraft.rns.recommend.AbstractRecommender;

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
    public void predict(DataInstance instance) {
        instance.setQuantityMark(meanScore);
    }

}
