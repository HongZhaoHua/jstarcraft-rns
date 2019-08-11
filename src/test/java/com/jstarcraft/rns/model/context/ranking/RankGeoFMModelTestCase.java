package com.jstarcraft.rns.model.context.ranking;

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
import com.jstarcraft.rns.model.context.ranking.RankGeoFMModel;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class RankGeoFMModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/recommend/context/ranking/rankgeofm-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(RankGeoFMModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.7268527F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.054826986F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.23780268F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.110460475F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(37.376965F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.07865529F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.08689054F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
