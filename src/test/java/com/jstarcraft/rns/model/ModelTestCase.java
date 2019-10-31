package com.jstarcraft.rns.model;

import com.jstarcraft.ai.modem.ModemCodec;
import com.jstarcraft.rns.task.AbstractTask;

public abstract class ModelTestCase {

    public void testModem(AbstractTask job) {
        for (ModemCodec codec : ModemCodec.values()) {
            Model oldModel = job.getModel();
            byte[] data = codec.encodeModel(oldModel);
            Model newModel = (Model) codec.decodeModel(data);
//            Assert.assertThat(newModel.predict(new int[] { 0, 1 }, new float[] {}), CoreMatchers.equalTo(oldModel.predict(new int[] { 0, 1 }, new float[] {})));
        }
    }

}
