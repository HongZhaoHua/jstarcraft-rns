package com.jstarcraft.rns.model.dl4j.ranking;

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
import com.jstarcraft.core.common.configuration.Configurator;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

//TODO 存档,以后需要基于DL4J重构.
@Deprecated
public class DeepFMModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/filmtrust.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/ranking/deepfm-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(DeepFMModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.916794F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.4057996F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.5699482F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.509845F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(11.902421F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.3271896F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.6142564F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
