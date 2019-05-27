package com.jstarcraft.rns.recommender.benchmark.rating;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.rns.configurator.Configuration;
import com.jstarcraft.rns.evaluator.rating.MAEEvaluator;
import com.jstarcraft.rns.evaluator.rating.MPEEvaluator;
import com.jstarcraft.rns.evaluator.rating.MSEEvaluator;
import com.jstarcraft.rns.recommender.benchmark.rating.ItemAverageRecommender;
import com.jstarcraft.rns.task.RatingTask;

public class ItemAverageTestCase {

    @Test
    public void testRecommender() throws Exception {
        Configuration configuration = Configuration.valueOf("recommendation/benchmark/itemaverage-test.properties");
        RatingTask job = new RatingTask(ItemAverageRecommender.class, configuration);
        Map<String, Float> measures = job.execute();
        Assert.assertThat(measures.get(MAEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.7237908F));
        Assert.assertThat(measures.get(MPEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.9780282F));
        Assert.assertThat(measures.get(MSEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.86158824F));
    }

}
