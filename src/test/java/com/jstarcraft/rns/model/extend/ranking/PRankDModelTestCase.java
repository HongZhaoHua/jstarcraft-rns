package com.jstarcraft.rns.model.extend.ranking;

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
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class PRankDModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/extend/prankd-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(PRankDModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.66697943F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.2072082F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.24065056F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.23053573F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(56.92662F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.19029133F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.23387833F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
