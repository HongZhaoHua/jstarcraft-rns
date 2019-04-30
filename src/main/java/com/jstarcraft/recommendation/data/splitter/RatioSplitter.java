package com.jstarcraft.recommendation.data.splitter;

import com.jstarcraft.ai.utility.IntegerArray;
import com.jstarcraft.recommendation.data.accessor.DenseModule;
import com.jstarcraft.recommendation.data.processor.DataMatcher;
import com.jstarcraft.recommendation.data.processor.DataSorter;

/**
 * 比率处理器
 * 
 * @author Birdy
 *
 */
public class RatioSplitter implements DataSplitter {

	private DenseModule dataModel;

	private IntegerArray trainReference;

	private IntegerArray testReference;

	public RatioSplitter(DenseModule model, String matchField, String sortField, double ratio) {
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
			int matchDimension = model.getQualityDimension(matchField);
			paginations = new int[model.getQualityAttribute(matchDimension).getSize() + 1];
			DataMatcher matcher = DataMatcher.discreteOf(model, matchDimension);
			matcher.match(paginations, positions);
		}
		if (model.getQualityFields().contains(sortField)) {
			int sortDimension = model.getQualityDimension(sortField);
			DataSorter sorter = DataSorter.discreteOf(model, sortDimension);
			sorter.sort(paginations, positions);
		} else if (model.getQuantityFields().contains(sortField)) {
			int sortDimension = model.getQuantityDimension(sortField);
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