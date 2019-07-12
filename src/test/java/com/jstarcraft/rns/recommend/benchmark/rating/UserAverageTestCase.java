package com.jstarcraft.rns.recommend.benchmark.rating;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.rns.configurator.Configuration;
import com.jstarcraft.rns.evaluate.rating.MAEEvaluator;
import com.jstarcraft.rns.evaluate.rating.MPEEvaluator;
import com.jstarcraft.rns.evaluate.rating.MSEEvaluator;
import com.jstarcraft.rns.recommend.benchmark.rating.UserAverageRecommender;
import com.jstarcraft.rns.task.RatingTask;

public class UserAverageTestCase {

	@Test
	public void testRecommender() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/benchmark/useraverage-test.properties");
		RatingTask job = new RatingTask(UserAverageRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(MAEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.63494015F));
		Assert.assertThat(measures.get(MPEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.97732395F));
		Assert.assertThat(measures.get(MSEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.6789832F));
	}

}
