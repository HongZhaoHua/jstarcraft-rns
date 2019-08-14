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
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class RankCDModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/product.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/ranking/rankcd-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(RankCDModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.5627095F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.012531294F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.04618105F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.026821574F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(55.42019F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.015480171F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.0351956F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
