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

public class AoBPRModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/filmtrust.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/ranking/aobpr-test.properties"));
        Option configuration = new MapOption(keyValues);
        RankingTask job = new RankingTask(AoBPRModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.8932367F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.38967326F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.53990144F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.48337516F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(21.130045F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.3229456F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.5686421F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
