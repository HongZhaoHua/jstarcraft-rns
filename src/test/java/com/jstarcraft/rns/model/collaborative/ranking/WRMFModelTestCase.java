package com.jstarcraft.rns.model.collaborative.ranking;

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

public class WRMFModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/recommend/collaborative/ranking/wrmf-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(WRMFModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.93334574F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.47451776F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.6280845F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.5691779F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(15.35682F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.3505674F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.63372076F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
