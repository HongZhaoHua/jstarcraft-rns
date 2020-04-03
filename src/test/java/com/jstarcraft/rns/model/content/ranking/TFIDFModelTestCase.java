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
import com.jstarcraft.core.common.configuration.Configurator;
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
        Assert.assertEquals(0.52755725F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.010667085F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.019169327F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.01772937F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(72.71228F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.005876948F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.03102854F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
