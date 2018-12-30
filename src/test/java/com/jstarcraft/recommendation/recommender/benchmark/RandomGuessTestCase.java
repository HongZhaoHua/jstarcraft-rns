package com.jstarcraft.recommendation.recommender.benchmark;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.ai.model.ModelCodec;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.evaluator.ranking.AUCEvaluator;
import com.jstarcraft.recommendation.evaluator.ranking.MAPEvaluator;
import com.jstarcraft.recommendation.evaluator.ranking.MRREvaluator;
import com.jstarcraft.recommendation.evaluator.ranking.NDCGEvaluator;
import com.jstarcraft.recommendation.evaluator.ranking.NoveltyEvaluator;
import com.jstarcraft.recommendation.evaluator.ranking.PrecisionEvaluator;
import com.jstarcraft.recommendation.evaluator.ranking.RecallEvaluator;
import com.jstarcraft.recommendation.evaluator.rating.MAEEvaluator;
import com.jstarcraft.recommendation.evaluator.rating.MPEEvaluator;
import com.jstarcraft.recommendation.evaluator.rating.MSEEvaluator;
import com.jstarcraft.recommendation.recommender.Recommender;
import com.jstarcraft.recommendation.task.RankingTask;
import com.jstarcraft.recommendation.task.RatingTask;

public class RandomGuessTestCase {

	@Test
	public void testRecommenderByRanking() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/benchmark/randomguess-test.properties");
		RankingTask job = new RankingTask(RandomGuessRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(AUCEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.5205934F));
		Assert.assertThat(measures.get(MAPEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.007114561F));
		Assert.assertThat(measures.get(MRREvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.023391832F));
		Assert.assertThat(measures.get(NDCGEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.012065685F));
		Assert.assertThat(measures.get(NoveltyEvaluator.class.getSimpleName()), CoreMatchers.equalTo(91.31491F));
		Assert.assertThat(measures.get(PrecisionEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.005825241F));
		Assert.assertThat(measures.get(RecallEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.011579763F));
		
		for (ModelCodec codec : ModelCodec.values()) {
			Recommender oldModel = job.getRecommender();
			byte[] data = codec.encodeModel(oldModel);
			Recommender newModel = (Recommender) codec.decodeModel(data);
			Assert.assertThat(newModel.predict(new int[] { 0, 1 }, new float[] {}), CoreMatchers.equalTo(oldModel.predict(new int[] { 0, 1 }, new float[] {})));
		}
	}

	@Test
	public void testRecommenderByRating() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/benchmark/randomguess-test.properties");
		RatingTask job = new RatingTask(RandomGuessRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(MAEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(1.2708743F));
		Assert.assertThat(measures.get(MPEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.9947887F));
		Assert.assertThat(measures.get(MSEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(2.425075F));

		for (ModelCodec codec : ModelCodec.values()) {
			Recommender oldModel = job.getRecommender();
			byte[] data = codec.encodeModel(oldModel);
			Recommender newModel = (Recommender) codec.decodeModel(data);
			Assert.assertThat(newModel.predict(new int[] { 0, 1 }, new float[] {}), CoreMatchers.equalTo(oldModel.predict(new int[] { 0, 1 }, new float[] {})));
		}
	}

}
