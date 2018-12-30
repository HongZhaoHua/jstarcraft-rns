package com.jstarcraft.recommendation.recommender.collaborative.ranking;

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
import com.jstarcraft.recommendation.task.RankingTask;

public class CLIMFTestCase {

	@Test
	public void testRecommender() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/collaborative/ranking/climf-test.properties");
		RankingTask job = new RankingTask(CLIMFRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(AUCEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.9179061F));
		Assert.assertThat(measures.get(MAPEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.43487352F));
		Assert.assertThat(measures.get(MRREvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.5912334F));
		Assert.assertThat(measures.get(NDCGEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.5304996F));
		Assert.assertThat(measures.get(NoveltyEvaluator.class.getSimpleName()), CoreMatchers.equalTo(15.530506F));
		Assert.assertThat(measures.get(PrecisionEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.3449845F));
		Assert.assertThat(measures.get(RecallEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.5955786F));
	}

}
