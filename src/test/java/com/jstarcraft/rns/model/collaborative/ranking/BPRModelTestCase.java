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
import com.jstarcraft.rns.model.collaborative.ranking.BPRModel;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class BPRModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/recommend/collaborative/ranking/bpr-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(BPRModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.9220709F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.45060882F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.60363245F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.545478F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(17.922415F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.34401378F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.61248535F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
