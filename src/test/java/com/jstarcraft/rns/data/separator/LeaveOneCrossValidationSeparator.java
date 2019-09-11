package com.jstarcraft.rns.data.separator;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.IntegerArray;
import com.jstarcraft.ai.data.module.ReferenceModule;
import com.jstarcraft.ai.data.processor.DataSorter;
import com.jstarcraft.ai.data.processor.DataSplitter;
import com.jstarcraft.rns.data.processor.QualityFeatureDataSorter;
import com.jstarcraft.rns.data.processor.QualityFeatureDataSplitter;
import com.jstarcraft.rns.data.processor.QuantityFeatureDataSorter;
import com.jstarcraft.rns.data.processor.RandomDataSorter;

/**
 * 留一验证分割器
 * 
 * @author Bridy
 *
 */
public class LeaveOneCrossValidationSeparator implements DataSeparator {

    private DataModule dataModule;

    private IntegerArray trainReference;

    private IntegerArray testReference;

    public LeaveOneCrossValidationSeparator(DataSpace space, DataModule dataModule, String matchField, String sortField) {
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
        DataSorter sorter;
        if (dataModule.getQualityInner(sortField) >= 0) {
            int sortDimension = dataModule.getQualityInner(sortField);
            sorter = new QualityFeatureDataSorter(sortDimension);
        } else if (dataModule.getQuantityInner(sortField) >= 0) {
            int sortDimension = dataModule.getQualityInner(sortField);
            sorter = new QuantityFeatureDataSorter(sortDimension);
        } else {
            sorter = new RandomDataSorter();
        }
        for (int index = 0, size = modules.length; index < size; index++) {
            IntegerArray oldReference = modules[index].getReference();
            IntegerArray newReference = sorter.sort(modules[index]).getReference();
            for (int cursor = 0, length = newReference.getSize(); cursor < length; cursor++) {
                newReference.setData(cursor, oldReference.getData(newReference.getData(cursor)));
            }
            modules[index] = new ReferenceModule(newReference, dataModule);
        }
        this.trainReference = new IntegerArray();
        this.testReference = new IntegerArray();
        for (ReferenceModule module : modules) {
            IntegerArray reference = module.getReference();
            for (int cursor = 0, length = reference.getSize(); cursor < length; cursor++) {
                if (length - cursor == 1) {
                    this.testReference.associateData(reference.getData(cursor));
                } else {
                    this.trainReference.associateData(reference.getData(cursor));
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
