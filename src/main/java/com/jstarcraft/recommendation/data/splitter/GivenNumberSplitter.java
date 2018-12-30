package com.jstarcraft.recommendation.data.splitter;

import com.jstarcraft.ai.utility.IntegerArray;
import com.jstarcraft.recommendation.data.accessor.InstanceAccessor;
import com.jstarcraft.recommendation.data.processor.DataMatcher;
import com.jstarcraft.recommendation.data.processor.DataSorter;

/**
 * 指定数量处理器
 * 
 * @author Birdy
 *
 */
// TODO 准备改名为SpecificNumberSplitter
public class GivenNumberSplitter implements DataSplitter {

	private InstanceAccessor dataModel;

	private IntegerArray trainReference;

	private IntegerArray testReference;

	public GivenNumberSplitter(InstanceAccessor model, String matchField, String sortField, int number) {
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
			int matchDimension = model.getDiscreteDimension(matchField);
			paginations = new int[model.getDiscreteAttribute(matchDimension).getSize() + 1];
			DataMatcher matcher = DataMatcher.discreteOf(model, matchDimension);
			matcher.match(paginations, positions);
		}
		if (model.getDiscreteFields().contains(sortField)) {
			int sortDimension = model.getDiscreteDimension(sortField);
			DataSorter sorter = DataSorter.discreteOf(model, sortDimension);
			sorter.sort(paginations, positions);
		} else if (model.getContinuousFields().contains(sortField)) {
			int sortDimension = model.getContinuousDimension(sortField);
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
			int count = 0;
			for (int from = paginations[index], to = paginations[index + 1]; from < to; from++) {
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
