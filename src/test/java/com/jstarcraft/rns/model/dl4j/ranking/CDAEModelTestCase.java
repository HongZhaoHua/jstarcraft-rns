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
import com.jstarcraft.core.common.configuration.MapConfigurator;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

// TODO 存档,以后需要基于DL4J重构.
@Deprecated
public class CDAEModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/filmtrust.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/ranking/cdae-test.properties"));
        Configurator configuration = new MapConfigurator(keyValues);
        RankingTask job = new RankingTask(CDAEModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.9188042F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.40759084F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.5685547F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.5108937F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(11.824657F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.3305053F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.61967427F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
