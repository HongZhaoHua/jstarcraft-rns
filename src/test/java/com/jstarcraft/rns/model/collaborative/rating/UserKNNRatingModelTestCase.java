package com.jstarcraft.rns.model.collaborative.rating;

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

public class UserKNNRatingModelTestCase {

    @Test
    public void testRecommenderRating() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/filmtrust.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/userknnrating-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RatingTask job = new RatingTask(UserKNNRatingModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.63933104F, measures.getFloat(MAEEvaluator.class), 0F);
        Assert.assertEquals(0.94639605F, measures.getFloat(MPEEvaluator.class), 0F);
        Assert.assertEquals(0.6927988F, measures.getFloat(MSEEvaluator.class), 0F);
    }

}
