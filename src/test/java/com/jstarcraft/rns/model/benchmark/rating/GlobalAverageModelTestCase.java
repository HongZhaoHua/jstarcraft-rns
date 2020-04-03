package com.jstarcraft.rns.model.benchmark.rating;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.ai.evaluate.Evaluator;
import com.jstarcraft.ai.evaluate.rating.MAEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MPEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MSEEvaluator;
import com.jstarcraft.core.common.configuration.Configurator;
import com.jstarcraft.rns.task.RatingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class GlobalAverageModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/filmtrust.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/benchmark/globalaverage-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RatingTask job = new RatingTask(GlobalAverageModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.71976596F, measures.getFloat(MAEEvaluator.class), 0F);
        Assert.assertEquals(0.77907884F, measures.getFloat(MPEEvaluator.class), 0F);
        Assert.assertEquals(0.85198635F, measures.getFloat(MSEEvaluator.class), 0F);
    }

}
