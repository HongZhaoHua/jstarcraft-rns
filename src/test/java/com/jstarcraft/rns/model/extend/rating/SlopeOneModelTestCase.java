package com.jstarcraft.rns.model.extend.rating;

import java.util.Properties;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.ai.evaluate.Evaluator;
import com.jstarcraft.ai.evaluate.rating.MAEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MPEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MSEEvaluator;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.rns.task.RatingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class SlopeOneModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/extend/slopeone-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RatingTask job = new RatingTask(SlopeOneModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.6333391F, measures.getFloat(MAEEvaluator.class), 0F);
        Assert.assertEquals(0.9653521F, measures.getFloat(MPEEvaluator.class), 0F);
        Assert.assertEquals(0.714156F, measures.getFloat(MSEEvaluator.class), 0F);
    }

}
