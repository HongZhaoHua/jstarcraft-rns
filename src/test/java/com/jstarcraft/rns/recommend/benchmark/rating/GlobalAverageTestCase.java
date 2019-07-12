package com.jstarcraft.rns.recommend.benchmark.rating;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.rns.configurator.Configuration;
import com.jstarcraft.rns.evaluate.rating.MAEEvaluator;
import com.jstarcraft.rns.evaluate.rating.MPEEvaluator;
import com.jstarcraft.rns.evaluate.rating.MSEEvaluator;
import com.jstarcraft.rns.recommend.benchmark.rating.GlobalAverageRecommender;
import com.jstarcraft.rns.task.RatingTask;

public class GlobalAverageTestCase {

    @Test
    public void testRecommender() throws Exception {
        Configuration configuration = Configuration.valueOf("recommendation/benchmark/globalaverage-test.properties");
        RatingTask job = new RatingTask(GlobalAverageRecommender.class, configuration);
        Map<String, Float> measures = job.execute();
        Assert.assertThat(measures.get(MAEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.7087578F));
        Assert.assertThat(measures.get(MPEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.7722535F));
        Assert.assertThat(measures.get(MSEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.83724F));
    }

}
