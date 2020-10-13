package com.jstarcraft.rns.model.collaborative.ranking;

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
import com.jstarcraft.core.common.option.MapOption;
import com.jstarcraft.core.common.option.Option;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class WBPRModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/filmtrust.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/ranking/wbpr-test.properties"));
        Option configuration = new MapOption(keyValues);
        RankingTask job = new RankingTask(WBPRModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.78071666F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.24647275F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.3337322F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.30442417F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(17.18609F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.25000066F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.35516423F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
