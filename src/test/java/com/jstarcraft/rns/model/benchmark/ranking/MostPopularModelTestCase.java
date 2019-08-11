package com.jstarcraft.rns.model.benchmark.ranking;

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

public class MostPopularModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/recommend/benchmark/mostpopular-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(MostPopularModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.9350321F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.45963627F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.6255547F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.56058705F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(11.643683F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.35186186F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.63336444F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
