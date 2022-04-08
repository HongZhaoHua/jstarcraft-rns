package com.jstarcraft.rns.model.context.ranking;

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
import com.jstarcraft.core.common.configuration.MapOption;
import com.jstarcraft.core.common.configuration.Option;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class RankGeoFMModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/Foursquare.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/context/ranking/rankgeofm-test.properties"));
        Option configuration = new MapOption(keyValues);
        RankingTask job = new RankingTask(RankGeoFMModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.7270785F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.054851912F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.2401193F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.110572465F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(37.500404F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.07865529F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.08640095F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
