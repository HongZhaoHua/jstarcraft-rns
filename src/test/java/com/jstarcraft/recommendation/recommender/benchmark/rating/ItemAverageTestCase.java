package com.jstarcraft.recommendation.recommender.benchmark.rating;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.ai.model.ModelCodec;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.evaluator.rating.MAEEvaluator;
import com.jstarcraft.recommendation.evaluator.rating.MPEEvaluator;
import com.jstarcraft.recommendation.evaluator.rating.MSEEvaluator;
import com.jstarcraft.recommendation.recommender.Recommender;
import com.jstarcraft.recommendation.task.RatingTask;

/**
 * Item Average Test Case correspond to ItemAverageRecommender
 * {@link com.jstarcraft.recommendation.recommender.benchmark.rating.librec.recommender.baseline.ItemAverageRecommender}
 * 
 * @author liuxz
 */
public class ItemAverageTestCase {

	@Test
	public void testRecommender() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/benchmark/itemaverage-test.properties");
		RatingTask job = new RatingTask(ItemAverageRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(MAEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.7237908F));
		Assert.assertThat(measures.get(MPEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.9780282F));
		Assert.assertThat(measures.get(MSEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.86158824F));

		for (ModelCodec codec : ModelCodec.values()) {
			Recommender oldModel = job.getRecommender();
			byte[] data = codec.encodeModel(oldModel);
			Recommender newModel = (Recommender) codec.decodeModel(data);
			Assert.assertThat(newModel.predict(new int[] { 0, 1 }, new float[] {}), CoreMatchers.equalTo(oldModel.predict(new int[] { 0, 1 }, new float[] {})));
		}
	}

}
