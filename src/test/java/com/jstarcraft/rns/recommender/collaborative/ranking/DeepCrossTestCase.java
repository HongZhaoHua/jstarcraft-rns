package com.jstarcraft.rns.recommender.collaborative.ranking;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.rns.configurator.Configuration;
import com.jstarcraft.rns.evaluator.ranking.AUCEvaluator;
import com.jstarcraft.rns.evaluator.ranking.MAPEvaluator;
import com.jstarcraft.rns.evaluator.ranking.MRREvaluator;
import com.jstarcraft.rns.evaluator.ranking.NDCGEvaluator;
import com.jstarcraft.rns.evaluator.ranking.NoveltyEvaluator;
import com.jstarcraft.rns.evaluator.ranking.PrecisionEvaluator;
import com.jstarcraft.rns.evaluator.ranking.RecallEvaluator;
import com.jstarcraft.rns.recommender.neuralnetwork.DeepCrossRecommender;
import com.jstarcraft.rns.task.RankingTask;

public class DeepCrossTestCase {

    @Test
    public void testRecommender() throws Exception {
        Configuration configuration = Configuration.valueOf("recommendation/collaborative/ranking/deepcross-test.properties");
        RankingTask job = new RankingTask(DeepCrossRecommender.class, configuration);
        Map<String, Float> measures = job.execute();
        Assert.assertThat(measures.get(AUCEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.93336445F));
        Assert.assertThat(measures.get(MAPEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.42737266F));
        Assert.assertThat(measures.get(MRREvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.5714847F));
        Assert.assertThat(measures.get(NDCGEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.53064024F));
        Assert.assertThat(measures.get(NoveltyEvaluator.class.getSimpleName()), CoreMatchers.equalTo(12.210984F));
        Assert.assertThat(measures.get(PrecisionEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.34393305F));
        Assert.assertThat(measures.get(RecallEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.61881465F));
    }

}
