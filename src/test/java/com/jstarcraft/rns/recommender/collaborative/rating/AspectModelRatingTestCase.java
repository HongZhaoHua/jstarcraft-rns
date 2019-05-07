package com.jstarcraft.rns.recommender.collaborative.rating;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.rns.configurator.Configuration;
import com.jstarcraft.rns.evaluator.rating.MAEEvaluator;
import com.jstarcraft.rns.evaluator.rating.MPEEvaluator;
import com.jstarcraft.rns.evaluator.rating.MSEEvaluator;
import com.jstarcraft.rns.recommender.collaborative.rating.AspectModelRatingRecommender;
import com.jstarcraft.rns.task.RatingTask;

public class AspectModelRatingTestCase {

	@Test
	public void testRecommender() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/collaborative/rating/aspectmodelrating-test.properties");
		RatingTask job = new RatingTask(AspectModelRatingRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(MAEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.6625144F));
		Assert.assertThat(measures.get(MPEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.9749296F));
		Assert.assertThat(measures.get(MSEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.7330673F));
	}

}
