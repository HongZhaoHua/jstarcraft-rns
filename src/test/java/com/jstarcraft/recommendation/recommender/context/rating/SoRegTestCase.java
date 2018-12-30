package com.jstarcraft.recommendation.recommender.context.rating;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.evaluator.rating.MAEEvaluator;
import com.jstarcraft.recommendation.evaluator.rating.MPEEvaluator;
import com.jstarcraft.recommendation.evaluator.rating.MSEEvaluator;
import com.jstarcraft.recommendation.task.RatingTask;

public class SoRegTestCase {

	@Test
	public void testRecommender() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/context/rating/soreg-test.properties");
		RatingTask job = new RatingTask(SoRegRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(MAEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.641287F));
		Assert.assertThat(measures.get(MPEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.96056336F));
		Assert.assertThat(measures.get(MSEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.7062128F));
	}

}
