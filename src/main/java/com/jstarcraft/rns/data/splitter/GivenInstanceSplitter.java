package com.jstarcraft.rns.data.splitter;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.module.ReferenceModule;
import com.jstarcraft.ai.utility.IntegerArray;
import com.jstarcraft.rns.data.processor.DataSelector;

/**
 * 指定实例处理器
 * 
 * @author Birdy
 *
 */
// TODO 准备改名为SpecificInstanceSplitter
public class GivenInstanceSplitter implements DataSplitter {

    private DataModule dataModel;

    private IntegerArray trainReference;

    private IntegerArray testReference;

    public GivenInstanceSplitter(DataModule model, DataSelector selector) {
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
    public DataModule getDataModel() {
        return dataModel;
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
