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
import com.jstarcraft.rns.model.collaborative.ranking.AspectModelRankingModel;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class AspectModelRankingModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/ranking/aspectmodelranking-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(AspectModelRankingModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.9289594F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.45355427F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.6159721F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.55231506F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(11.742993F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.3510527F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.6224215F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
