package com.jstarcraft.rns.model.collaborative.rating;

import java.util.Map;
import java.util.Properties;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.ai.evaluate.Evaluator;
import com.jstarcraft.ai.evaluate.rating.MAEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MPEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MSEEvaluator;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.rns.model.collaborative.rating.RFRecModel;
import com.jstarcraft.rns.task.RatingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class RFRecModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/recommend/collaborative/rating/rfrec-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RatingTask job = new RatingTask(RFRecModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.63200635F, measures.getFloat(MAEEvaluator.class), 0F);
        Assert.assertEquals(0.9728169F, measures.getFloat(MPEEvaluator.class), 0F);
        Assert.assertEquals(0.6844252F, measures.getFloat(MSEEvaluator.class), 0F);
    }

}