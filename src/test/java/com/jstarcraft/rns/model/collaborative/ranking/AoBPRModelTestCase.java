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
import com.jstarcraft.rns.model.collaborative.ranking.AoBPRModel;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class AoBPRModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/ranking/aobpr-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(AoBPRModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.9195788F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.43754834F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.5987676F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.53109217F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(17.760157F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.33980674F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.58792263F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
