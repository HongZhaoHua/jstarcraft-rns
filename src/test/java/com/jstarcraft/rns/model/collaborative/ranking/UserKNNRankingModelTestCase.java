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
import com.jstarcraft.rns.model.collaborative.ranking.UserKNNRankingModel;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class UserKNNRankingModelTestCase {

    @Test
    public void testRecommenderRanking() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/userknnranking-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(UserKNNRankingModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.9361596F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.47433883F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.6388499F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.5715007F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(13.84465F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.34725013F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.635084F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
