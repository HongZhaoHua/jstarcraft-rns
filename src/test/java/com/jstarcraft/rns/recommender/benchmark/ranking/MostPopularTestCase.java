package com.jstarcraft.rns.recommender.benchmark.ranking;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.ai.modem.ModemCodec;
import com.jstarcraft.rns.configurator.Configuration;
import com.jstarcraft.rns.evaluator.ranking.AUCEvaluator;
import com.jstarcraft.rns.evaluator.ranking.MAPEvaluator;
import com.jstarcraft.rns.evaluator.ranking.MRREvaluator;
import com.jstarcraft.rns.evaluator.ranking.NDCGEvaluator;
import com.jstarcraft.rns.evaluator.ranking.NoveltyEvaluator;
import com.jstarcraft.rns.evaluator.ranking.PrecisionEvaluator;
import com.jstarcraft.rns.evaluator.ranking.RecallEvaluator;
import com.jstarcraft.rns.recommender.Recommender;
import com.jstarcraft.rns.recommender.benchmark.ranking.MostPopularRecommender;
import com.jstarcraft.rns.task.RankingTask;

public class MostPopularTestCase {

    @Test
    public void testRecommender() throws Exception {
        Configuration configuration = Configuration.valueOf("recommendation/benchmark/mostpopular-test.properties");
        RankingTask job = new RankingTask(MostPopularRecommender.class, configuration);
        Map<String, Float> measures = job.execute();
        Assert.assertThat(measures.get(AUCEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.9350321F));
        Assert.assertThat(measures.get(MAPEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.45963627F));
        Assert.assertThat(measures.get(MRREvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.6255547F));
        Assert.assertThat(measures.get(NDCGEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.56058705F));
        Assert.assertThat(measures.get(NoveltyEvaluator.class.getSimpleName()), CoreMatchers.equalTo(11.643683F));
        Assert.assertThat(measures.get(PrecisionEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.35186186F));
        Assert.assertThat(measures.get(RecallEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.63336444F));
    }

}
