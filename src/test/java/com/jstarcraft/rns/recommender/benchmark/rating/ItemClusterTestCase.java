package com.jstarcraft.rns.recommender.benchmark.rating;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.rns.configurator.Configuration;
import com.jstarcraft.rns.evaluator.rating.MAEEvaluator;
import com.jstarcraft.rns.evaluator.rating.MPEEvaluator;
import com.jstarcraft.rns.evaluator.rating.MSEEvaluator;
import com.jstarcraft.rns.recommender.benchmark.rating.ItemClusterRecommender;
import com.jstarcraft.rns.task.RatingTask;

public class ItemClusterTestCase {

	@Test
	public void testRecommender() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/benchmark/itemcluster-test.properties");
		RatingTask job = new RatingTask(ItemClusterRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(MAEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.70878124F));
		Assert.assertThat(measures.get(MPEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.7722535F));
		Assert.assertThat(measures.get(MSEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.8372418F));
	}

}
