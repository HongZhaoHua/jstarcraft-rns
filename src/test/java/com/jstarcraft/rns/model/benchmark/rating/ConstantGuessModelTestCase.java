package com.jstarcraft.rns.model.benchmark.rating;

import java.util.Map;
import java.util.Properties;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.ai.evaluate.Evaluator;
import com.jstarcraft.ai.evaluate.rating.MAEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MPEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MSEEvaluator;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.rns.model.benchmark.rating.ConstantGuessModel;
import com.jstarcraft.rns.task.RatingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class ConstantGuessModelTestCase {

	@Test
	public void testRecommender() throws Exception {
	    Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/model/benchmark/constantguess-test.properties"));
        Configurator configuration = new Configurator(keyValues);
		RatingTask job = new RatingTask(ConstantGuessModel.class, configuration);
		Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
		Assert.assertEquals(1.0565493F, measures.getFloat(MAEEvaluator.class), 0F);
		Assert.assertEquals(1.0F, measures.getFloat(MPEEvaluator.class), 0F);
		Assert.assertEquals(1.4247535F, measures.getFloat(MSEEvaluator.class), 0F);
	}

}
