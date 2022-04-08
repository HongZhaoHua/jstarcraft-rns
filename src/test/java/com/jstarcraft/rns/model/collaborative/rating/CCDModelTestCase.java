package com.jstarcraft.rns.model.collaborative.rating;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.ai.evaluate.Evaluator;
import com.jstarcraft.ai.evaluate.rating.MAEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MPEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MSEEvaluator;
import com.jstarcraft.core.common.configuration.MapOption;
import com.jstarcraft.core.common.configuration.Option;
import com.jstarcraft.rns.task.RatingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class CCDModelTestCase {

	@Test
	public void testRecommender() throws Exception {
		Properties keyValues = new Properties();
		keyValues.load(this.getClass().getResourceAsStream("/data/product.properties"));
		keyValues.load(this.getClass().getResourceAsStream("/model/collaborative/rating/ccd-test.properties"));
		Option configuration = new MapOption(keyValues);
		RatingTask job = new RatingTask(CCDModel.class, configuration);
		Object2FloatSortedMap<Class<? extends Evaluator>> measures = job.execute();
		Assert.assertEquals(0.9645193F, measures.getFloat(MAEEvaluator.class), 0F);
		Assert.assertEquals(0.9366471F, measures.getFloat(MPEEvaluator.class), 0F);
		Assert.assertEquals(1.6167855F, measures.getFloat(MSEEvaluator.class), 0F);
	}

}
