package com.jstarcraft.rns.data.separator;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.IntegerArray;
import com.jstarcraft.ai.data.module.ReferenceModule;
import com.jstarcraft.rns.data.processor.DataMatcher;
import com.jstarcraft.rns.data.processor.DataOrder;

/**
 * 比率处理器
 * 
 * @author Birdy
 *
 */
public class RatioSeparator implements DataSeparator {

    private DataModule dataModel;

    private IntegerArray trainReference;

    private IntegerArray testReference;

    public RatioSeparator(DataSpace space, DataModule model, String matchField, String sortField, double ratio) {
        dataModel = model;
        int size = model.getSize();
        int[] paginations;
        int[] positions = new int[size];
        for (int index = 0; index < size; index++) {
            positions[index] = index;
        }
        if (matchField == null) {
            paginations = new int[] { 0, size };
        } else {
            int matchDimension = model.getQualityInner(matchField);
            paginations = new int[space.getQualityAttribute(matchField).getSize() + 1];
            DataMatcher matcher = DataMatcher.discreteOf(model, matchDimension);
            matcher.match(paginations, positions);
        }
        if (model.getQualityInner(sortField) >= 0) {
            int sortDimension = model.getQualityInner(sortField);
            DataOrder sorter = DataOrder.discreteOf(model, sortDimension);
            sorter.sort(paginations, positions);
        } else if (model.getQuantityInner(sortField) >= 0) {
            int sortDimension = model.getQuantityInner(sortField);
            DataOrder sorter = DataOrder.continuousOf(model, sortDimension);
            sorter.sort(paginations, positions);
        } else {
            DataOrder sorter = DataOrder.RANDOM_SORTER;
            sorter.sort(paginations, positions);
        }

        trainReference = new IntegerArray();
        testReference = new IntegerArray();
        size = paginations.length - 1;
        for (int index = 0; index < size; index++) {
            int from = paginations[index], to = paginations[index + 1];
            int count = 0;
            int number = (int) ((to - from) * ratio);
            for (; from < to; from++) {
                if (count++ < number) {
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