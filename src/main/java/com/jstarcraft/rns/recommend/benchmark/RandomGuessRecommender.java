package com.jstarcraft.rns.recommend.benchmark;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.modem.ModemDefinition;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.recommend.AbstractRecommender;

/**
 * 
 * Random Guess推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
@ModemDefinition(value = { "userDimension", "itemDimension", "numberOfItems", "minimumOfScore", "maximumOfScore" })
public class RandomGuessRecommender extends AbstractRecommender {

    @Override
    protected void doPractice() {
    }

    @Override
    public synchronized float predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        RandomUtility.setSeed(userIndex * numberOfItems + itemIndex);
        return RandomUtility.randomFloat(minimumOfScore, maximumOfScore);
    }

}
