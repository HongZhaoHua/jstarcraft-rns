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
import com.jstarcraft.rns.model.collaborative.ranking.RankVFCDModel;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class RankVFCDModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/ranking/rankvfcd-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(RankVFCDModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.5802234F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.017840676F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.061810836F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.036641084F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(62.9581F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.019802151F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.04851758F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
