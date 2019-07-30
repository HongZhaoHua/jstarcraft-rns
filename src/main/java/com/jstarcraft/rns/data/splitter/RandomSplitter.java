package com.jstarcraft.rns.data.splitter;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.IntegerArray;
import com.jstarcraft.ai.data.module.ReferenceModule;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.data.processor.DataMatcher;

/**
 * 随机处理器
 * 
 * @author Birdy
 *
 */
public class RandomSplitter implements DataSplitter {

    private DataModule dataModel;

    private IntegerArray trainReference;

    private IntegerArray testReference;

    public RandomSplitter(DataSpace space, DataModule model, String matchField, double random) {
        dataModel = model;
        int size = model.getSize();
        int[] paginations;
        int[] positions = new int[size];
        for (int index = 0; index < size; index++) {
            positions[index] = index;
        }
        int matchDimension = model.getQualityInner(matchField);
        paginations = new int[space.getQualityAttribute(matchField).getSize() + 1];
        DataMatcher matcher = DataMatcher.discreteOf(model, matchDimension);
        matcher.match(paginations, positions);

        trainReference = new IntegerArray();
        testReference = new IntegerArray();
        size = paginations.length - 1;
        for (int index = 0; index < size; index++) {
            for (int from = paginations[index], to = paginations[index + 1]; from < to; from++) {
                if (RandomUtility.randomDouble(1D) < random) {
                    trainReference.associateData(positions[from]);
                } else {
                    testReference.associateData(positions[from]);
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