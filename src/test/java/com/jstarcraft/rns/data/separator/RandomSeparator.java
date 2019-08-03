package com.jstarcraft.rns.data.separator;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.IntegerArray;
import com.jstarcraft.ai.data.module.ReferenceModule;
import com.jstarcraft.ai.data.processor.DataSplitter;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.data.processor.QualityFeatureDataSplitter;

/**
 * 随机处理器
 * 
 * @author Birdy
 *
 */
public class RandomSeparator implements DataSeparator {

    private DataModule dataModel;

    private IntegerArray trainReference;

    private IntegerArray testReference;

    public RandomSeparator(DataSpace space, DataModule model, String matchField, float random) {
        dataModel = model;
        ReferenceModule[] modules;
        if (matchField == null) {
            modules = new ReferenceModule[] { new ReferenceModule(model) };
        } else {
            int matchDimension = model.getQualityInner(matchField);
            DataSplitter splitter = new QualityFeatureDataSplitter(matchDimension);
            int size = space.getQualityAttribute(matchField).getSize();
            modules = splitter.split(model, size);
        }
        trainReference = new IntegerArray();
        testReference = new IntegerArray();
        for (ReferenceModule module : modules) {
            IntegerArray reference =  module.getReference();
            for (int cursor = 0, length = reference.getSize(); cursor < length; cursor++) {
                if (RandomUtility.randomFloat(1F) < random) {
                    trainReference.associateData(reference.getData(cursor));
                } else {
                    testReference.associateData(reference.getData(cursor));
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
        return new ReferenceModule(trainReference, dataModel);
    }

    @Override
    public ReferenceModule getTestReference(int index) {
        return new ReferenceModule(testReference, dataModel);
    }

}