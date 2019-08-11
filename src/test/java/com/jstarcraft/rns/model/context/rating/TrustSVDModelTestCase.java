package com.jstarcraft.rns.model.context.rating;

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

public class TrustSVDModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/recommend/context/rating/trustsvd-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RatingTask job = new RatingTask(TrustSVDModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.6098742F, measures.getFloat(MAEEvaluator.class), 0F);
        Assert.assertEquals(0.9860563F, measures.getFloat(MPEEvaluator.class), 0F);
        Assert.assertEquals(0.6233134F, measures.getFloat(MSEEvaluator.class), 0F);
    }

}
