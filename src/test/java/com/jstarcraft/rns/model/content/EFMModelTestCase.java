package com.jstarcraft.rns.model.content;

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
import com.jstarcraft.ai.evaluate.rating.MAEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MPEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MSEEvaluator;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.rns.model.content.ranking.EFMRankingModel;
import com.jstarcraft.rns.model.content.rating.EFMRatingModel;
import com.jstarcraft.rns.task.RankingTask;
import com.jstarcraft.rns.task.RatingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class EFMModelTestCase {

    @Test
    public void testRecommenderByRanking() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/content/efmranking-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RankingTask job = new RankingTask(EFMRankingModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.6127146F, measures.getFloat(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.01611203F, measures.getFloat(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.04630792F, measures.getFloat(MRREvaluator.class), 0F);
        Assert.assertEquals(0.040448334F, measures.getFloat(NDCGEvaluator.class), 0F);
        Assert.assertEquals(53.2614F, measures.getFloat(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.023869349F, measures.getFloat(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.073571086F, measures.getFloat(RecallEvaluator.class), 0F);
    }

    @Test
    public void testRecommenderByRating() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/content/efmrating-test.properties"));
        Configurator configuration = new Configurator(keyValues);
        RatingTask job = new RatingTask(EFMRatingModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.6154602F, measures.getFloat(MAEEvaluator.class), 0F);
        Assert.assertEquals(0.8536428F, measures.getFloat(MPEEvaluator.class), 0F);
        Assert.assertEquals(0.78278536F, measures.getFloat(MSEEvaluator.class), 0F);
    }

}
