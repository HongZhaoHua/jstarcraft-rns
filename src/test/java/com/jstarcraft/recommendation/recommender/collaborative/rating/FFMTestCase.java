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

public class FFMTestCase {

	@Test
	public void testRecommender() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/collaborative/rating/ffm-test.properties");
		RatingTask job = new RatingTask(FFMRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(MAEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.622898F));
		Assert.assertThat(measures.get(MPEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.9911268F));
		Assert.assertThat(measures.get(MSEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.64381474F));
	}

}
