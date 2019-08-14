package com.jstarcraft.rns.data.separator;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.IntegerArray;
import com.jstarcraft.ai.data.module.ReferenceModule;
import com.jstarcraft.ai.data.processor.DataSplitter;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.data.processor.QualityFeatureDataSplitter;

/**
 * 随机分割器
 * 
 * @author Birdy
 *
 */
public class RandomSeparator implements DataSeparator {

    private DataModule dataModule;

    private IntegerArray trainReference;

    private IntegerArray testReference;

    public RandomSeparator(DataSpace space, DataModule dataModule, String matchField, float random) {
        this.dataModule = dataModule;
        ReferenceModule[] modules;
        if (matchField == null) {
            modules = new ReferenceModule[] { new ReferenceModule(dataModule) };
        } else {
            int matchDimension = dataModule.getQualityInner(matchField);
            DataSplitter splitter = new QualityFeatureDataSplitter(matchDimension);
            int size = space.getQualityAttribute(matchField).getSize();
            modules = splitter.split(dataModule, size);
        }
        this.trainReference = new IntegerArray();
        this.testReference = new IntegerArray();
        for (ReferenceModule module : modules) {
            IntegerArray reference = module.getReference();
            for (int cursor = 0, length = reference.getSize(); cursor < length; cursor++) {
                if (RandomUtility.randomFloat(1F) < random) {
                    this.trainReference.associateData(reference.getData(cursor));
                } else {
                    this.testReference.associateData(reference.getData(cursor));
                }
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