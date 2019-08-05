package com.jstarcraft.rns.recommend.collaborative.ranking;

import java.util.Map.Entry;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.rns.recommend.collaborative.BUCMRecommender;

/**
 * 
 * BUCM推荐器
 * 
 * <pre>
 * Bayesian User Community Model
 * Modeling Item Selection and Relevance for Accurate Recommendations
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class BUCMRankingRecommender extends BUCMRecommender {

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        float value = 0F;
        for (int topicIndex = 0; topicIndex < factorSize; ++topicIndex) {
            float sum = 0F;
            for (Entry<Float, Integer> term : scoreIndexes.entrySet()) {
                double rate = term.getKey();
                if (rate > meanScore) {
                    sum += topicItemRateProbabilities[topicIndex][itemIndex][term.getValue()];
                }
            }
            value += userTopicProbabilities.getValue(userIndex, topicIndex) * topicItemProbabilities.getValue(topicIndex, itemIndex) * sum;
        }
        instance.setQuantityMark(value);
    }

}
