package com.jstarcraft.recommendation.data.processor;

import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.recommendation.data.DataAccessor;

/**
 * 数据排序器
 * 
 * @author Birdy
 *
 */
public interface DataSorter {

	public final static DataSorter RANDOM_SORTER = (paginations, positions) -> {
		for (int index = 0, size = paginations.length - 1; index < size; index++) {
			int from = paginations[index], to = paginations[index + 1];
			RandomUtility.shuffle(positions, from, to);
		}
	};

	/**
	 * 按照所有特征排序
	 * 
	 * @param accessor
	 * @return
	 */
	public static DataSorter featureOf(DataAccessor<?> accessor) {
		return (paginations, positions) -> {
			for (int index = 0, size = paginations.length - 1; index < size; index++) {
				int from = paginations[index], to = paginations[index + 1];
				for (int left = from; left < to - 1; left++) {
					for (int right = left + 1; right < to; right++) {
						// TODO 注意:此处存在0的情况.
						boolean change = false;
						for (int dimension = 0, order = accessor.getDiscreteOrder(); dimension < order; dimension++) {
							int leftValue = accessor.getDiscreteFeature(dimension, positions[left]);
							int rightValue = accessor.getDiscreteFeature(dimension, positions[right]);
							int value = leftValue - rightValue;
							if (value != 0) {
								if (leftValue > rightValue) {
									positions[left] ^= positions[right];
									positions[right] ^= positions[left];
									positions[left] ^= positions[right];
								}
								change = true;
								break;
							}
						}
						if (change) {
							continue;
						}
						for (int dimension = 0, order = accessor.getContinuousOrder(); dimension < order; dimension++) {
							double leftValue = accessor.getContinuousFeature(dimension, positions[left]);
							double rightValue = accessor.getContinuousFeature(dimension, positions[right]);
							double value = leftValue - rightValue;
							if (value != 0D) {
								if (leftValue > rightValue) {
									positions[left] ^= positions[right];
									positions[right] ^= positions[left];
									positions[left] ^= positions[right];
								}
								change = true;
								break;
							}
						}
						if (change) {
							continue;
						}
					}
				}
			}
		};
	}

	/**
	 * 按照指定的离散特征排序
	 * 
	 * @param accessor
	 * @param dimension
	 * @return
	 */
	public static DataSorter discreteOf(DataAccessor<?> accessor, int dimension) {
		return (paginations, positions) -> {
			for (int index = 0, size = paginations.length - 1; index < size; index++) {
				int from = paginations[index], to = paginations[index + 1];
				for (int left = from; left < to; left++) {
					for (int right = left + 1; right < to; right++) {
						// TODO 注意:此处存在0的情况.
						int leftValue = accessor.getDiscreteFeature(dimension, positions[left]);
						int rightValue = accessor.getDiscreteFeature(dimension, positions[right]);
						if (leftValue > rightValue) {
							positions[left] ^= positions[right];
							positions[right] ^= positions[left];
							positions[left] ^= positions[right];
						}
					}
				}
			}
		};
	}

	/**
	 * 按照指定的连续特征排序
	 * 
	 * @param accessor
	 * @param dimension
	 * @return
	 */
	public static DataSorter continuousOf(DataAccessor<?> accessor, int dimension) {
		return (paginations, positions) -> {
			for (int index = 0, size = paginations.length - 1; index < size; index++) {
				int from = paginations[index], to = paginations[index + 1];
				for (int left = from; left < to; left++) {
					for (int right = left + 1; right < to; right++) {
						// TODO 注意:此处存在0的情况.
						double leftValue = accessor.getContinuousFeature(dimension, positions[left]);
						double rightValue = accessor.getContinuousFeature(dimension, positions[right]);
						if (leftValue > rightValue) {
							positions[left] ^= positions[right];
							positions[right] ^= positions[left];
							positions[left] ^= positions[right];
						}
					}
				}
			}
		};
	}

	void sort(int[] paginations, int[] positions);

}
