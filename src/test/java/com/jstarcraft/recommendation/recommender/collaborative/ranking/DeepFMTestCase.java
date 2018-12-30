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
import com.jstarcraft.recommendation.recommender.neuralnetwork.DeepFMRecommender;
import com.jstarcraft.recommendation.task.RankingTask;

public class DeepFMTestCase {

	@Test
	public void testRecommender() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/collaborative/ranking/deepfm-test.properties");
		RankingTask job = new RankingTask(DeepFMRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(AUCEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.93613666F));
		Assert.assertThat(measures.get(MAPEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.43918133F));
		Assert.assertThat(measures.get(MRREvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.56956583F));
		Assert.assertThat(measures.get(NDCGEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.5408482F));
		Assert.assertThat(measures.get(NoveltyEvaluator.class.getSimpleName()), CoreMatchers.equalTo(11.88964F));
		Assert.assertThat(measures.get(PrecisionEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.35113364F));
		Assert.assertThat(measures.get(RecallEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.6336484F));
	}

}
