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
import com.jstarcraft.core.common.option.Option;
import com.jstarcraft.core.utility.Integer2FloatKeyValue;
import com.jstarcraft.rns.model.Model;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;

/**
 * 评分任务
 * 
 * @author Birdy
 *
 */
public class RatingTask extends AbstractTask<FloatList, FloatList> {

    public RatingTask(Model recommender, Option configuration) {
        super(recommender, configuration);
    }

    public RatingTask(Class<? extends Model> clazz, Option configuration) {
        super(clazz, configuration);
    }

    @Override
    protected Collection<Evaluator> getEvaluators(SparseMatrix featureMatrix) {
        Collection<Evaluator> evaluators = new LinkedList<>();
        float minimum = configurator.getFloat("recommender.recommender.rating.minimum", 0.5F);
        float maximum = configurator.getFloat("recommender.recommender.rating.maximum", 4F);
        evaluators.add(new MAEEvaluator(minimum, maximum));
        evaluators.add(new MPEEvaluator(minimum, maximum, 0.01F));
        evaluators.add(new MSEEvaluator(minimum, maximum));
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
    protected FloatList recommend(Model recommender, int userIndex) {
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
