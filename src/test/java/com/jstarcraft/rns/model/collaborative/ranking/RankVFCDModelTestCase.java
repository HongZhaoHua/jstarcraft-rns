package com.jstarcraft.rns.model.collaborative.ranking;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.ai.evaluate.Evaluator;
import com.jstarcraft.ai.evaluate.ranking.AUCEvaluator;
import com.jstarcraft.ai.evaluate.ranking.MAPEvaluator;
import com.jstarcraft.ai.evaluate.ranking.MRREvaluator;
import com.jstarcraft.ai.evaluate.ranking.NDCGEvaluator;
import com.jstarcraft.ai.evaluate.ranking.NoveltyEvaluator;
import com.jstarcraft.ai.evaluate.ranking.PrecisionEvaluator;
import com.jstarcraft.ai.evaluate.ranking.RecallEvaluator;
import com.jstarcraft.core.common.option.MapOption;
import com.jstarcraft.core.common.option.Option;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class RankVFCDModelTestCase {

	@Test
	public void testRecommender() throws Exception {
		Properties keyValues = new Properties();
		keyValues.load(this.getClass().getResourceAsStream("/data/product.properties"));
		keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/ranking/rankvfcd-test.properties"));
		Option configuration = new MapOption(keyValues);
		RankingTask job = new RankingTask(RankVFCDModel.class, configuration);
		Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
		Assert.assertEquals(0.5782429F, measures.getFloat(AUCEvaluator.class), 0F);
		Assert.assertEquals(0.019607447F, measures.getFloat(MAPEvaluator.class), 0F);
		Assert.assertEquals(0.06451471F, measures.getFloat(MRREvaluator.class), 0F);
		Assert.assertEquals(0.038099557F, measures.getFloat(NDCGEvaluator.class), 0F);
		Assert.assertEquals(61.21012F, measures.getFloat(NoveltyEvaluator.class), 0F);
		Assert.assertEquals(0.01949143F, measures.getFloat(PrecisionEvaluator.class), 0F);
		Assert.assertEquals(0.048476923F, measures.getFloat(RecallEvaluator.class), 0F);
	}

}
