package com.jstarcraft.rns.model.content.ranking;

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
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class TFIDFModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/musical_instruments.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/content/tfidf-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(TFIDFModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.5128588F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.003790126F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.007245251F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.007090965F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(77.49009F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.002662993F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.01318743F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
