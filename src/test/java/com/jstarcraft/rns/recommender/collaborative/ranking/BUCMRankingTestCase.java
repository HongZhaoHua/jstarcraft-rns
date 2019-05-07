package com.jstarcraft.rns.recommender.collaborative.ranking;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.rns.configurator.Configuration;
import com.jstarcraft.rns.evaluator.ranking.AUCEvaluator;
import com.jstarcraft.rns.evaluator.ranking.MAPEvaluator;
import com.jstarcraft.rns.evaluator.ranking.MRREvaluator;
import com.jstarcraft.rns.evaluator.ranking.NDCGEvaluator;
import com.jstarcraft.rns.evaluator.ranking.NoveltyEvaluator;
import com.jstarcraft.rns.evaluator.ranking.PrecisionEvaluator;
import com.jstarcraft.rns.evaluator.ranking.RecallEvaluator;
import com.jstarcraft.rns.recommender.collaborative.ranking.BUCMRankingRecommender;
import com.jstarcraft.rns.task.RankingTask;

public class BUCMRankingTestCase {

	@Test
	public void testRecommenderRanking() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/collaborative/bucmranking-test.properties");
		RankingTask job = new RankingTask(BUCMRankingRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(AUCEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.9342104F));
		Assert.assertThat(measures.get(MAPEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.45868912F));
		Assert.assertThat(measures.get(MRREvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.6186524F));
		Assert.assertThat(measures.get(NDCGEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.5577358F));
		Assert.assertThat(measures.get(NoveltyEvaluator.class.getSimpleName()), CoreMatchers.equalTo(12.828235F));
		Assert.assertThat(measures.get(PrecisionEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.35202366F));
		Assert.assertThat(measures.get(RecallEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.6302245F));
	}

}
