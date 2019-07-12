package com.jstarcraft.rns.evaluate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.module.ArrayInstance;
import com.jstarcraft.ai.utility.Integer2FloatKeyValue;
import com.jstarcraft.rns.recommend.Recommender;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public abstract class AbstractRankingEvaluatorTestCase extends AbstractEvaluatorTestCase<IntCollection> {

    @Override
    protected IntCollection check(int userIndex) {
        DataInstance instance = testMarker.getInstance(0);
        IntSet itemSet = new IntOpenHashSet();
        int from = testPaginations[userIndex], to = testPaginations[userIndex + 1];
        for (int index = from, size = to; index < size; index++) {
            int position = testPositions[index];
            instance.setCursor(position);
            itemSet.add(instance.getQualityFeature(itemDimension));
        }
        return itemSet;
    }

    @Override
    protected List<Integer2FloatKeyValue> recommend(Recommender recommender, int userIndex) {
        DataInstance instance = trainMarker.getInstance(0);
        Set<Integer> itemSet = new HashSet<>();
        int from = trainPaginations[userIndex], to = trainPaginations[userIndex + 1];
        for (int index = from, size = to; index < size; index++) {
            int position = trainPositions[index];
            instance.setCursor(position);
            itemSet.add(instance.getQualityFeature(itemDimension));
        }
        // TODO 此处代码需要重构
        ArrayInstance copy = new ArrayInstance(trainMarker.getQualityOrder(), trainMarker.getQuantityOrder());
        if (from < to) {
            instance.setCursor(trainPositions[to - 1]);
            copy.copyInstance(instance);
        } else {
            for (int index = 0; index < copy.getQualityOrder(); index++) {
                copy.setQualityFeature(index, 0);
            }
            for (int index = 0; index < copy.getQuantityOrder(); index++) {
                copy.setQuantityFeature(index, 0F);
            }
        }
        copy.setQualityFeature(userDimension, userIndex);
        List<Integer2FloatKeyValue> recommendList = new ArrayList<>(numberOfItems - itemSet.size());
        for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
            if (itemSet.contains(itemIndex)) {
                continue;
            }
            copy.setQualityFeature(itemDimension, itemIndex);
            recommendList.add(new Integer2FloatKeyValue(itemIndex, recommender.predict(copy)));
        }
        Collections.sort(recommendList, (left, right) -> {
            return Float.compare(right.getValue(), left.getValue());
        });
        return recommendList;
    }

}
