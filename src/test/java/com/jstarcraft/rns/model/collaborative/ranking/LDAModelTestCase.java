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
import com.jstarcraft.core.common.configuration.MapOption;
import com.jstarcraft.core.common.configuration.Option;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class LDAModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/filmtrust.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/ranking/lda-test.properties"));
        Option configuration = new MapOption(keyValues);
        RankingTask job = new RankingTask(LDAModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.919801F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.41758165F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.58130056F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.5200285F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(12.313484F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.33335692F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.62273633F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
