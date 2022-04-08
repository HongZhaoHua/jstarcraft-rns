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

public class CLiMFModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/filmtrust.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/ranking/climf-test.properties"));
        Configurator configuration = new MapConfigurator(keyValues);
        RankingTask job = new RankingTask(CLiMFModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.8829291F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.3739544F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.5240671F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.4657167F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(19.389643F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.32049185F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.5460514F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
