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

public class AspectModelRankingModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/filmtrust.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/ranking/aspectmodelranking-test.properties"));
        Configurator configuration = new MapConfigurator(keyValues);
        RankingTask job = new RankingTask(AspectModelRankingModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.8513018F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.15497543F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.42479676F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.2601204F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(37.362732F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.13302413F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.31291583F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
