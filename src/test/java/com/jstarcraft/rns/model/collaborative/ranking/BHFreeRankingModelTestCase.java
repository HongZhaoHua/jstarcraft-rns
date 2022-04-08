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
import com.jstarcraft.core.common.configuration.Configurator;
import com.jstarcraft.core.common.configuration.string.MapConfigurator;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class BHFreeRankingModelTestCase {

    @Test
    public void testRecommenderRanking() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/filmtrust.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/bhfreeranking-test.properties"));
        Configurator configuration = new MapConfigurator(keyValues);
        RankingTask job = new RankingTask(BHFreeRankingModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.9208008F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.413161F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.5723087F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.51661724F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(11.79567F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.33276004F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.62499523F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
