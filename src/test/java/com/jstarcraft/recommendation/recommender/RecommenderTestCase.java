package com.jstarcraft.recommendation.recommender;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;

import com.jstarcraft.ai.modem.ModemCodec;
import com.jstarcraft.recommendation.task.AbstractTask;

public abstract class RecommenderTestCase {

    public void testModem(AbstractTask job) {
        for (ModemCodec codec : ModemCodec.values()) {
            Recommender oldModel = job.getRecommender();
            byte[] data = codec.encodeModel(oldModel);
            Recommender newModel = (Recommender) codec.decodeModel(data);
            Assert.assertThat(newModel.predict(new int[] { 0, 1 }, new float[] {}), CoreMatchers.equalTo(oldModel.predict(new int[] { 0, 1 }, new float[] {})));
        }
    }

}
