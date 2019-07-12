package com.jstarcraft.rns.recommend.collaborative.rating;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.rns.configurator.Configuration;
import com.jstarcraft.rns.evaluate.rating.MAEEvaluator;
import com.jstarcraft.rns.evaluate.rating.MPEEvaluator;
import com.jstarcraft.rns.evaluate.rating.MSEEvaluator;
import com.jstarcraft.rns.recommend.collaborative.rating.ItemKNNRatingRecommender;
import com.jstarcraft.rns.task.RatingTask;

public class ItemKNNRatingTestCase {

	@Test
	public void testRecommenderRating() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/collaborative/itemknnrating-test.properties");
		RatingTask job = new RatingTask(ItemKNNRatingRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(MAEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.6194126F));
		Assert.assertThat(measures.get(MPEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.96633804F));
		Assert.assertThat(measures.get(MSEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.6818529F));
	}

}
