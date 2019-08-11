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
import com.jstarcraft.rns.model.neuralnetwork.DeepCrossModel;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class DeepCrossModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/recommend/collaborative/ranking/deepcross-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(DeepCrossModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.93662477F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.4330167F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.5726203F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.53728354F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(12.432615F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.34773567F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.6300728F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
