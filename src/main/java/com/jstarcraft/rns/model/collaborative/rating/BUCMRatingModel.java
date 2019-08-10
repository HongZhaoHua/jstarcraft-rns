package com.jstarcraft.rns.model.collaborative.rating;

import java.util.Map.Entry;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.rns.model.collaborative.BUCMModel;

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
public class BUCMRatingModel extends BUCMModel {

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        float value = 0F, probabilities = 0F;
        for (Entry<Float, Integer> term : scoreIndexes.entrySet()) {
            float score = term.getKey();
            float probability = 0F;
            for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                probability += userTopicProbabilities.getValue(userIndex, topicIndex) * topicItemProbabilities.getValue(topicIndex, itemIndex) * topicItemScoreProbabilities[topicIndex][itemIndex][term.getValue()];
            }
            value += probability * score;
            probabilities += probability;
        }
        instance.setQuantityMark(value / probabilities);
    }

}
