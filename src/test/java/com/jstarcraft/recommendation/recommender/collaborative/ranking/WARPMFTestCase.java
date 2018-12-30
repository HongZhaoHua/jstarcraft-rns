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

public class WARPMFTestCase {

	@Test
	public void testRecommender() throws Exception {
		Configuration configuration = Configuration.valueOf("recommendation/collaborative/ranking/warpmf-test.properties");
		RankingTask job = new RankingTask(WARPMFRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(AUCEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.911843F));
		Assert.assertThat(measures.get(MAPEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.4421523F));
		Assert.assertThat(measures.get(MRREvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.6031782F));
		Assert.assertThat(measures.get(NDCGEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.5340485F));
		Assert.assertThat(measures.get(NoveltyEvaluator.class.getSimpleName()), CoreMatchers.equalTo(17.189053F));
		Assert.assertThat(measures.get(PrecisionEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.34166747F));
		Assert.assertThat(measures.get(RecallEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.586479F));
	}

}
