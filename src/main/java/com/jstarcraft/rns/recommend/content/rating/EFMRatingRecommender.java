package com.jstarcraft.rns.recommend.content.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.rns.recommend.content.EFMRecommender;

/**
 * 
 * User KNN推荐器
 * 
 * <pre>
 * Explicit factor models for explainable recommendation based on phrase-level sentiment analysis
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class EFMRatingRecommender extends EFMRecommender {

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        float value = predict(userIndex, itemIndex);
        if (value < minimumOfScore) {
            instance.setQuantityMark(minimumOfScore);
            return;
        }
        if (value > maximumOfScore) {
            instance.setQuantityMark(maximumOfScore);
            return;
        }
        instance.setQuantityMark(value);
    }

}
