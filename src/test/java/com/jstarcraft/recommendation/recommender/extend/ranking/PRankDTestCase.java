package com.jstarcraft.recommendation.recommender.extend.ranking;

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

public class PRankDTestCase {

	@Test
	public void testRecommender() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/extend/prankd-test.properties");
		RankingTask job = new RankingTask(PRankDRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(AUCEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.6669868F));
		Assert.assertThat(measures.get(MAPEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.2072082F));
		Assert.assertThat(measures.get(MRREvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.24065056F));
		Assert.assertThat(measures.get(NDCGEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.23053573F));
		Assert.assertThat(measures.get(NoveltyEvaluator.class.getSimpleName()), CoreMatchers.equalTo(56.92863F));
		Assert.assertThat(measures.get(PrecisionEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.19029133F));
		Assert.assertThat(measures.get(RecallEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.23387833F));
	}

}
