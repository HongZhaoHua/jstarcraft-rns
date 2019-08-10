package com.jstarcraft.rns.model;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;

import com.jstarcraft.ai.modem.ModemCodec;
import com.jstarcraft.rns.model.Model;
import com.jstarcraft.rns.task.AbstractTask;

public abstract class ModelTestCase {

    public void testModem(AbstractTask job) {
        for (ModemCodec codec : ModemCodec.values()) {
            Model oldModel = job.getRecommender();
            byte[] data = codec.encodeModel(oldModel);
            Model newModel = (Model) codec.decodeModel(data);
//            Assert.assertThat(newModel.predict(new int[] { 0, 1 }, new float[] {}), CoreMatchers.equalTo(oldModel.predict(new int[] { 0, 1 }, new float[] {})));
        }
    }

}
