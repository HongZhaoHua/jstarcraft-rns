package com.jstarcraft.rns.model.extend.rating;

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

public class SlopeOneModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/filmtrust.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/extend/slopeone-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RatingTask job = new RatingTask(SlopeOneModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.6378848F, measures.getFloat(MAEEvaluator.class), 0F);
        Assert.assertEquals(0.96174866F, measures.getFloat(MPEEvaluator.class), 0F);
        Assert.assertEquals(0.7105687F, measures.getFloat(MSEEvaluator.class), 0F);
    }

}
