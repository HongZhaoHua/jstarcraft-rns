package com.jstarcraft.rns.model.collaborative.ranking;

import java.util.Map;
import java.util.Properties;

import org.hamcrest.CoreMatchers;
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
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.rns.model.collaborative.ranking.WBPRModel;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class WBPRModelTestCase {

	@Test
	public void testRecommender() throws Exception {
	    Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/recommend/collaborative/ranking/wbpr-test.properties"));
        Configurator configuration = new Configurator(keyValues);
		RankingTask job = new RankingTask(WBPRModel.class, configuration);
		Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
		Assert.assertEquals(0.808611F,measures.getFloat(AUCEvaluator.class), 0F);
		Assert.assertEquals(0.28209546F,measures.getFloat(MAPEvaluator.class), 0F);
		Assert.assertEquals(0.37461984F,measures.getFloat(MRREvaluator.class), 0F);
		Assert.assertEquals(0.34037653F,measures.getFloat(NDCGEvaluator.class), 0F);
		Assert.assertEquals(17.189224F,measures.getFloat(NoveltyEvaluator.class), 0F);
		Assert.assertEquals(0.26593906F,measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.37083453F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
