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
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/recommend/benchmark/randomguess-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(RandomGuessModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.5205948F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.007114561F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.023391832F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.012065685F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(91.31491F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.005825241F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.011579763F, measures.getFloat(RecallEvaluator.class), 0F);
    }

    @Test
    public void testRecommenderByRating() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/recommend/benchmark/randomguess-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RatingTask job = new RatingTask(RandomGuessModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(1.2708743F, measures.getFloat(MAEEvaluator.class), 0F);
        Assert.assertEquals(0.9947887F, measures.getFloat(MPEEvaluator.class), 0F);
        Assert.assertEquals(2.425075F, measures.getFloat(MSEEvaluator.class), 0F);
    }

}
