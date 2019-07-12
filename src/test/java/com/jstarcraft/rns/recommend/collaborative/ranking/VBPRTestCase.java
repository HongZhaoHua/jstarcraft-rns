package com.jstarcraft.rns.recommend.collaborative.ranking;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.rns.configure.Configuration;
import com.jstarcraft.rns.evaluate.ranking.AUCEvaluator;
import com.jstarcraft.rns.evaluate.ranking.MAPEvaluator;
import com.jstarcraft.rns.evaluate.ranking.MRREvaluator;
import com.jstarcraft.rns.evaluate.ranking.NDCGEvaluator;
import com.jstarcraft.rns.evaluate.ranking.NoveltyEvaluator;
import com.jstarcraft.rns.evaluate.ranking.PrecisionEvaluator;
import com.jstarcraft.rns.evaluate.ranking.RecallEvaluator;
import com.jstarcraft.rns.recommend.collaborative.ranking.VBPRRecommender;
import com.jstarcraft.rns.task.RankingTask;

public class VBPRTestCase {

	@Test
	public void testRecommender() throws Exception {
		Configuration configuration = Configuration.valueOf("recommend/collaborative/ranking/vbpr-test.properties");
		RankingTask job = new RankingTask(VBPRRecommender.class, configuration);
		Map<String, Float> measures = job.execute();
		Assert.assertThat(measures.get(AUCEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.54307634F));
		Assert.assertThat(measures.get(MAPEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.009206047F));
		Assert.assertThat(measures.get(MRREvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.035549607F));
		Assert.assertThat(measures.get(NDCGEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.018847404F));
		Assert.assertThat(measures.get(NoveltyEvaluator.class.getSimpleName()), CoreMatchers.equalTo(45.424107F));
		Assert.assertThat(measures.get(PrecisionEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.010338988F));
		Assert.assertThat(measures.get(RecallEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.022509828F));
	}

}