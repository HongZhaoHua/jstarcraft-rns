package com.jstarcraft.rns.model.extend.ranking;

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
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class AssociationRuleModelTestCase {

    @Test
    public void testAssociationRuleRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/extend/associationrule-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(AssociationRuleModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.933426F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.47252607F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.64005077F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.5708647F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(12.263066F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.35323712F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.6358811F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
