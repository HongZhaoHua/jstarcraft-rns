package com.jstarcraft.rns.data.separator;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.IntegerArray;
import com.jstarcraft.ai.data.module.ReferenceModule;
import com.jstarcraft.core.utility.RandomUtility;

/**
 * K折叠交叉分割器
 *
 * @author Birdy
 */
public class KFoldCrossValidationSeparator implements DataSeparator {

    private DataModule dataModel;

    private Integer[] folds;

    private int number;

    public KFoldCrossValidationSeparator(DataModule model, int number) {
        dataModel = model;
        this.number = number;
        folds = new Integer[dataModel.getSize()];
        for (int index = 0, size = folds.length; index < size; index++) {
            folds[index] = index % number;
        }
        // 通过随机与交换的方式实现打乱排序的目的.
        RandomUtility.shuffle(folds);
    }

    @Override
    public int getSize() {
        return number;
    }

    @Override
    public ReferenceModule getTrainReference(int index) {
        IntegerArray reference = new IntegerArray();
        for (int position = 0, size = dataModel.getSize(); position < size; position++) {
            if (folds[position] != index) {
                reference.associateData(position);
            }
        }
        return new ReferenceModule(reference, dataModel);
    }

    @Override
    public ReferenceModule getTestReference(int index) {
        IntegerArray reference = new IntegerArray();
        for (int position = 0, size = dataModel.getSize(); position < size; position++) {
            if (folds[position] == index) {
                reference.associateData(position);
            }
        }
        return new ReferenceModule(reference, dataModel);
    }

}
