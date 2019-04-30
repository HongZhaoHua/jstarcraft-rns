package com.jstarcraft.recommendation.data.splitter;

import com.jstarcraft.ai.utility.IntegerArray;
import com.jstarcraft.recommendation.data.accessor.DataInstance;
import com.jstarcraft.recommendation.data.accessor.DenseModule;
import com.jstarcraft.recommendation.data.processor.DataSelector;

/**
 * 指定实例处理器
 * 
 * @author Birdy
 *
 */
// TODO 准备改名为SpecificInstanceSplitter
public class GivenInstanceSplitter implements DataSplitter {

	private DenseModule dataModel;

	private IntegerArray trainReference;

	private IntegerArray testReference;

	public GivenInstanceSplitter(DenseModule model, DataSelector selector) {
		this.dataModel = model;

		this.trainReference = new IntegerArray();
		this.testReference = new IntegerArray();
		int position = 0;
		for (DataInstance instance : model) {
			if (selector.select(instance)) {
				testReference.associateData(position++);
			} else {
				trainReference.associateData(position++);
			}
		}
	}

	@Override
	public int getSize() {
		return 1;
	}

	@Override
	public DenseModule getDataModel() {
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
