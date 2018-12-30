package com.jstarcraft.recommendation.recommender.collaborative.rating;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.evaluator.rating.MAEEvaluator;
import com.jstarcraft.recommendation.evaluator.rating.MPEEvaluator;
import com.jstarcraft.recommendation.evaluator.rating.MSEEvaluator;
import com.jstarcraft.recommendation.task.RatingTask;

public class FMALSTestCase {

	@Test
	public void testRecommender() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/collaborative/rating/fmals-test.properties");
		RatingTask job = new RatingTask(FMALSRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(MAEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.6445985F));
		Assert.assertThat(measures.get(MPEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.9897183F));
		Assert.assertThat(measures.get(MSEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.73218936F));
	}

}
