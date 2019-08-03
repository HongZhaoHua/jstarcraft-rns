package com.jstarcraft.rns.data.processor;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.IntegerArray;
import com.jstarcraft.ai.data.module.ReferenceModule;
import com.jstarcraft.ai.data.processor.DataSorter;
import com.jstarcraft.core.utility.RandomUtility;

public class RandomDataSorter implements DataSorter {

    @Override
    public int sort(DataInstance left, DataInstance right) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ReferenceModule sort(DataModule module) {
        int size = module.getSize();
        IntegerArray reference = new IntegerArray(size, size);
        for (int index = 0; index < size; index++) {
            reference.associateData(index);
        }
        int from = 0;
        int to = size;
        for (int index = from; index < to; index++) {
            int random = RandomUtility.randomInteger(from, to);
            int data = reference.getData(index);
            reference.setData(index, reference.getData(random));
            reference.setData(random, data);
        }
        return new ReferenceModule(reference, module);
    }

}
