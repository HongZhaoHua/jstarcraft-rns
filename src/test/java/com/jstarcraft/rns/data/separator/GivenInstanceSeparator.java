package com.jstarcraft.rns.data.separator;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.IntegerArray;
import com.jstarcraft.ai.data.module.ReferenceModule;
import com.jstarcraft.ai.data.processor.DataSelector;

/**
 * 指定实例分割器
 * 
 * @author Birdy
 *
 */
public class GivenInstanceSeparator implements DataSeparator {

    private DataModule dataModel;

    private IntegerArray trainReference;

    private IntegerArray testReference;

    public GivenInstanceSeparator(DataModule model, DataSelector selector) {
        this.dataModel = model;

        this.trainReference = new IntegerArray();
        this.testReference = new IntegerArray();
        int position = 0;
        for (DataInstance instance : model) {
            if (selector.select(instance)) {
                testReference.associateData(position++);
            } else {
                trainReference.associateData(position++);
            }
        }
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public ReferenceModule getTrainReference(int index) {
        return new ReferenceModule(trainReference, dataModel);
    }

    @Override
    public ReferenceModule getTestReference(int index) {
        return new ReferenceModule(testReference, dataModel);
    }

}
