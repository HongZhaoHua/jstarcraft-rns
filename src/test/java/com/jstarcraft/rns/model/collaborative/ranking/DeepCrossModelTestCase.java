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
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.rns.model.neuralnetwork.DeepCrossModel;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class DeepCrossModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/filmtrust.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/ranking/deepcross-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(DeepCrossModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.91646796F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.39583597F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.5631354F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.50070804F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(12.073422F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.32440427F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.60906875F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
