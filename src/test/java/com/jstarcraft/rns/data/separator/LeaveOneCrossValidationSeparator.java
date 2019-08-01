package com.jstarcraft.rns.data.separator;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.IntegerArray;
import com.jstarcraft.ai.data.module.ReferenceModule;
import com.jstarcraft.rns.data.processor.DataMatcher;
import com.jstarcraft.rns.data.processor.DataSorter;

/**
 * 留一验证处理器
 * 
 * @author Bridy
 *
 */
public class LeaveOneCrossValidationSeparator implements DataSeparator {

    private DataModule dataModel;

    private IntegerArray trainReference;

    private IntegerArray testReference;

    public LeaveOneCrossValidationSeparator(DataSpace space, DataModule model, String matchField, String sortField) {
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
            DataSorter sorter = DataSorter.discreteOf(model, sortDimension);
            sorter.sort(paginations, positions);
        } else if (model.getQuantityInner(sortField) >= 0) {
            int sortDimension = model.getQuantityInner(sortField);
            DataSorter sorter = DataSorter.continuousOf(model, sortDimension);
            sorter.sort(paginations, positions);
        } else {
            DataSorter sorter = DataSorter.RANDOM_SORTER;
            sorter.sort(paginations, positions);
        }

        trainReference = new IntegerArray();
        testReference = new IntegerArray();
        size = paginations.length - 1;
        for (int index = 0; index < size; index++) {
            int from = paginations[index], to = paginations[index + 1];
            if (to - from >= 1) {
                testReference.associateData(positions[--to]);
            }
            for (; from < to; from++) {
                trainReference.associateData(positions[from]);
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
