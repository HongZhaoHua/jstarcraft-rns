package com.jstarcraft.rns.model.benchmark.rating;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.ai.evaluate.Evaluator;
import com.jstarcraft.ai.evaluate.rating.MAEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MPEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MSEEvaluator;
import com.jstarcraft.core.common.option.MapOption;
import com.jstarcraft.core.common.option.Option;
import com.jstarcraft.rns.task.RatingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class ItemClusterModelTestCase {

    @Test
    public void testRecommender() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data/filmtrust.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/benchmark/itemcluster-test.properties"));
        Option configuration = new MapOption(keyValues);
        RatingTask job = new RatingTask(ItemClusterModel.class, configuration);
        Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
        Assert.assertEquals(0.7197603F, measures.getFloat(MAEEvaluator.class), 0F);
        Assert.assertEquals(0.77907884F, measures.getFloat(MPEEvaluator.class), 0F);
        Assert.assertEquals(0.8519833F, measures.getFloat(MSEEvaluator.class), 0F);
    }

}
