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
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class PLSAModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/filmtrust.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/ranking/plsa-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(PLSAModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.8995006F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.41217065F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.5718696F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.5059695F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(16.010801F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.32400653F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.5855675F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
