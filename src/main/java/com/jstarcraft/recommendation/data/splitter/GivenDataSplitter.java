package com.jstarcraft.recommendation.data.splitter;

import com.jstarcraft.ai.utility.IntegerArray;
import com.jstarcraft.recommendation.data.accessor.InstanceAccessor;

/**
 * 指定数据处理器
 * 
 * @author Birdy
 *
 */
// TODO 准备改名为SpecificThresholdSplitter
public class GivenDataSplitter implements DataSplitter {

	private InstanceAccessor dataModel;

	private IntegerArray trainReference;

	private IntegerArray testReference;

	public GivenDataSplitter(InstanceAccessor model, int threshold) {
		dataModel = model;
		trainReference = new IntegerArray();
		testReference = new IntegerArray();
		int size = model.getSize();
		for (int index = 0; index < size; index++) {
			if (index < threshold) {
				trainReference.associateData(index);
			} else {
				testReference.associateData(index);
			}
		}
	}

	@Override
	public int getSize() {
		return 1;
	}

	@Override
	public InstanceAccessor getDataModel() {
		return dataModel;
	}

	@Override
	public IntegerArray getTrainReference(int index) {
		return trainReference;
	}

	@Override
	public IntegerArray getTestReference(int index) {
		return testReference;
	}

}
