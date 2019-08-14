package com.jstarcraft.rns.data.separator;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.IntegerArray;
import com.jstarcraft.ai.data.module.ReferenceModule;

/**
 * 指定数据分割器
 * 
 * @author Birdy
 *
 */
public class GivenDataSeparator implements DataSeparator {

    private DataModule dataModule;

    private IntegerArray trainReference;

    private IntegerArray testReference;

    public GivenDataSeparator(DataModule dataModule, int threshold) {
        this.dataModule = dataModule;
        this.trainReference = new IntegerArray();
        this.testReference = new IntegerArray();
        int size = dataModule.getSize();
        for (int index = 0; index < size; index++) {
            if (index < threshold) {
                this.trainReference.associateData(index);
            } else {
                this.testReference.associateData(index);
            }
        }
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public ReferenceModule getTrainReference(int index) {
        return new ReferenceModule(trainReference, dataModule);
    }

    @Override
    public ReferenceModule getTestReference(int index) {
        return new ReferenceModule(testReference, dataModule);
    }

}
