package com.jstarcraft.rns.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.module.ArrayInstance;
import com.jstarcraft.ai.data.module.ReferenceModule;
import com.jstarcraft.ai.evaluate.Evaluator;
import com.jstarcraft.ai.evaluate.rating.MAEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MPEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MSEEvaluator;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.core.utility.Integer2FloatKeyValue;
import com.jstarcraft.rns.configure.Configurator;
import com.jstarcraft.rns.recommend.Recommender;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;

/**
 * 评分任务
 * 
 * @author Birdy
 *
 */
public class RatingTask extends AbstractTask<FloatList, FloatList> {

    public RatingTask(Recommender recommender, Configurator configuration) {
        super(recommender, configuration);
    }

    public RatingTask(Class<? extends Recommender> clazz, Configurator configuration) {
        super(clazz, configuration);
    }

    @Override
    protected Collection<Evaluator> getEvaluators(SparseMatrix featureMatrix) {
        Collection<Evaluator> evaluators = new LinkedList<>();
        evaluators.add(new MAEEvaluator());
        evaluators.add(new MPEEvaluator(0.01F));
        evaluators.add(new MSEEvaluator());
        return evaluators;
    }

    @Override
    protected FloatList check(int userIndex) {
        ReferenceModule testModule = testModules[userIndex];
        FloatList scoreList = new FloatArrayList(testModule.getSize());
        for (DataInstance instance : testModule) {
            scoreList.add(instance.getQuantityMark());
        }
        return scoreList;
    }

    @Override
    protected FloatList recommend(Recommender recommender, int userIndex) {
        ReferenceModule testModule = testModules[userIndex];
        ArrayInstance copy = new ArrayInstance(testMarker.getQualityOrder(), testMarker.getQuantityOrder());
        List<Integer2FloatKeyValue> rateList = new ArrayList<>(testModule.getSize());
        for (DataInstance instance : testModule) {
            copy.copyInstance(instance);
            recommender.predict(copy);
            rateList.add(new Integer2FloatKeyValue(copy.getQualityFeature(itemDimension), copy.getQuantityMark()));
        }

        FloatList recommendList = new FloatArrayList(rateList.size());
        for (Integer2FloatKeyValue keyValue : rateList) {
            recommendList.add(keyValue.getValue());
        }
        return recommendList;
    }

}
