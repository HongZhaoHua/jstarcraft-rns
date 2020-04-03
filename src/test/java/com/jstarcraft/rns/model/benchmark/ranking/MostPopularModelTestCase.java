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
import com.jstarcraft.core.common.configuration.Configurator;
import com.jstarcraft.core.common.configuration.MapConfigurator;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class MostPopularModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/filmtrust.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/benchmark/mostpopular-test.properties"));
        Configurator configuration = new MapConfigurator(keyValues);
        RankingTask job = new RankingTask(MostPopularModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.9207961F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.4124602F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.571964F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.5158319F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(11.792954F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.33229586F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.62384576F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
