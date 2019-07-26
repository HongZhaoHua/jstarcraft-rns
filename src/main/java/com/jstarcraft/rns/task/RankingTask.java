package com.jstarcraft.rns.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.module.ArrayInstance;
import com.jstarcraft.ai.evaluate.Evaluator;
import com.jstarcraft.ai.evaluate.ranking.AUCEvaluator;
import com.jstarcraft.ai.evaluate.ranking.MAPEvaluator;
import com.jstarcraft.ai.evaluate.ranking.MRREvaluator;
import com.jstarcraft.ai.evaluate.ranking.NDCGEvaluator;
import com.jstarcraft.ai.evaluate.ranking.NoveltyEvaluator;
import com.jstarcraft.ai.evaluate.ranking.PrecisionEvaluator;
import com.jstarcraft.ai.evaluate.ranking.RecallEvaluator;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.utility.Integer2FloatKeyValue;
import com.jstarcraft.rns.configure.Configurator;
import com.jstarcraft.rns.recommend.Recommender;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * 排序任务
 * 
 * @author Birdy
 *
 */
public class RankingTask extends AbstractTask<IntSet, IntList> {

    public RankingTask(Class<? extends Recommender> clazz, Configurator configuration) {
        super(clazz, configuration);
    }

    @Override
    protected Collection<Evaluator> getEvaluators(SparseMatrix featureMatrix) {
        Collection<Evaluator> evaluators = new LinkedList<>();
        int size = configuration.getInteger("recommender.recommender.ranking.topn", 10);
        evaluators.add(new AUCEvaluator(size));
        evaluators.add(new MAPEvaluator(size));
        evaluators.add(new MRREvaluator(size));
        evaluators.add(new NDCGEvaluator(size));
        evaluators.add(new NoveltyEvaluator(size, featureMatrix));
        evaluators.add(new PrecisionEvaluator(size));
        evaluators.add(new RecallEvaluator(size));
        return evaluators;
    }

    @Override
    protected IntSet check(int userIndex) {
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
    protected IntList recommend(Recommender recommender, int userIndex) {
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

        List<Integer2FloatKeyValue> rankList = new ArrayList<>(numberOfItems - itemSet.size());
        for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
            if (itemSet.contains(itemIndex)) {
                continue;
            }
            copy.setQualityFeature(itemDimension, itemIndex);
            recommender.predict(copy);
            rankList.add(new Integer2FloatKeyValue(itemIndex, copy.getQuantityMark()));
        }
        Collections.sort(rankList, (left, right) -> {
            return Float.compare(right.getValue(), left.getValue());
        });

        IntList recommendList = new IntArrayList(rankList.size());
        for (Integer2FloatKeyValue keyValue : rankList) {
            recommendList.add(keyValue.getKey());
        }
        return recommendList;
    }

}
