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
import com.jstarcraft.rns.recommender.collaborative.ranking.BHFreeRankingRecommender;
import com.jstarcraft.rns.task.RankingTask;

public class BHFreeRankingTestCase {

	@Test
	public void testRecommenderRanking() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/collaborative/bhfreeranking-test.properties");
		RankingTask job = new RankingTask(BHFreeRankingRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(AUCEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.9354352F));
		Assert.assertThat(measures.get(MAPEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.45921868F));
		Assert.assertThat(measures.get(MRREvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.6249835F));
		Assert.assertThat(measures.get(NDCGEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.56032866F));
		Assert.assertThat(measures.get(NoveltyEvaluator.class.getSimpleName()), CoreMatchers.equalTo(11.64368F));
		Assert.assertThat(measures.get(PrecisionEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.35178095F));
		Assert.assertThat(measures.get(RecallEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.63397115F));
	}

}
