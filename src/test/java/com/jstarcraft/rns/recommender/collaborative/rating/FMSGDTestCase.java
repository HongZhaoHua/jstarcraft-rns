package com.jstarcraft.rns.recommender.collaborative.rating;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.rns.configurator.Configuration;
import com.jstarcraft.rns.evaluator.rating.MAEEvaluator;
import com.jstarcraft.rns.evaluator.rating.MPEEvaluator;
import com.jstarcraft.rns.evaluator.rating.MSEEvaluator;
import com.jstarcraft.rns.recommender.collaborative.rating.FMSGDRecommender;
import com.jstarcraft.rns.task.RatingTask;

public class FMSGDTestCase {

	@Test
	public void testRecommender() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/collaborative/rating/fmsgd-test.properties");
		RatingTask job = new RatingTask(FMSGDRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(MAEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.62297696F));
		Assert.assertThat(measures.get(MPEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.99070424F));
		Assert.assertThat(measures.get(MSEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.64387435F));
	}

}
