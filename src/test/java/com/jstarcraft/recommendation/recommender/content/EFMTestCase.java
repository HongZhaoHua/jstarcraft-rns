package com.jstarcraft.recommendation.recommender.content;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

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
import com.jstarcraft.recommendation.recommender.content.ranking.EFMRankingRecommender;
import com.jstarcraft.recommendation.recommender.content.rating.EFMRatingRecommender;
import com.jstarcraft.recommendation.task.RankingTask;
import com.jstarcraft.recommendation.task.RatingTask;

public class EFMTestCase {

	@Test
	public void testRecommenderByRanking() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/content/efmranking-test.properties");
		RankingTask job = new RankingTask(EFMRankingRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(AUCEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.61280894F));
		Assert.assertThat(measures.get(MAPEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.01611203F));
		Assert.assertThat(measures.get(MRREvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.04630792F));
		Assert.assertThat(measures.get(NDCGEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.040448334F));
		Assert.assertThat(measures.get(NoveltyEvaluator.class.getSimpleName()), CoreMatchers.equalTo(53.2614F));
		Assert.assertThat(measures.get(PrecisionEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.023869349F));
		Assert.assertThat(measures.get(RecallEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.073571086F));
	}

	@Test
	public void testRecommenderByRating() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/content/efmrating-test.properties");
		RatingTask job = new RatingTask(EFMRatingRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(MAEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.6154602F));
		Assert.assertThat(measures.get(MPEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.8536428F));
		Assert.assertThat(measures.get(MSEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.78278536F));
	}

}
