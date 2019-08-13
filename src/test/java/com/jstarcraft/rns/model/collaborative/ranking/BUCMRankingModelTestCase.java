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
import com.jstarcraft.rns.model.collaborative.ranking.BUCMRankingModel;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class BUCMRankingModelTestCase {

    @Test
    public void testRecommenderRanking() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/bucmranking-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(BUCMRankingModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.9297708F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.45527673F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.62023246F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.55345F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(12.848778F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.34886828F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.6196705F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
