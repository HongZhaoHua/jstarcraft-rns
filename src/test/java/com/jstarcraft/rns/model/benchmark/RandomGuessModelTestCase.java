package com.jstarcraft.rns.model.benchmark;

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
import com.jstarcraft.ai.evaluate.rating.MAEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MPEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MSEEvaluator;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.rns.model.benchmark.RandomGuessModel;
import com.jstarcraft.rns.task.RankingTask;
import com.jstarcraft.rns.task.RatingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class RandomGuessModelTestCase {

    @Test
    public void testRecommenderByRanking() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/filmtrust.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/benchmark/randomguess-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(RandomGuessModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.5192176F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.006268634F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.021699615F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.01120969F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(91.949F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.0055039763F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.012620986F, measures.getFloat(RecallEvaluator.class), 0F);
    }

    @Test
    public void testRecommenderByRating() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/filmtrust.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/benchmark/randomguess-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RatingTask job = new RatingTask(RandomGuessModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(1.2862209F, measures.getFloat(MAEEvaluator.class), 0F);
        Assert.assertEquals(0.9959667F, measures.getFloat(MPEEvaluator.class), 0F);
        Assert.assertEquals(2.479267F, measures.getFloat(MSEEvaluator.class), 0F);
    }

}
