package com.jstarcraft.rns.model.collaborative.ranking;

import java.util.Map;
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
import com.jstarcraft.rns.model.collaborative.ranking.LambdaFMDynamicModel;
import com.jstarcraft.rns.model.collaborative.ranking.LambdaFMStaticModel;
import com.jstarcraft.rns.model.collaborative.ranking.LambdaFMWeightModel;
import com.jstarcraft.rns.task.RankingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class LambdaFMModelTestCase {

    @Test
    public void testRecommenderByDynamic() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/ranking/lambdafmd-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(LambdaFMDynamicModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.8738025F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.27287653F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.43647555F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.34705582F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(13.505785F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.13822167F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.35131538F, measures.getFloat(RecallEvaluator.class), 0F);
    }

    @Test
    public void testRecommenderByStatic() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/ranking/lambdafms-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(LambdaFMStaticModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.87063825F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.27293852F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.43640044F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.34793553F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(16.4733F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.13940796F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.3569557F, measures.getFloat(RecallEvaluator.class), 0F);
    }

    @Test
    public void testRecommenderByWeight() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/ranking/lambdafmw-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(LambdaFMWeightModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.87338704F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.27333382F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.4372049F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.34727877F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(14.714127F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.13741651F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.35251862F, measures.getFloat(RecallEvaluator.class), 0F);
    }

}
