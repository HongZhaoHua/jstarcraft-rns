package com.jstarcraft.rns.data.processor;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.core.utility.RandomUtility;

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
    public static DataSorter featureOf(DataModule accessor) {
        DataInstance instance = accessor.getInstance(0);
        return (paginations, positions) -> {
            for (int index = 0, size = paginations.length - 1; index < size; index++) {
                int from = paginations[index], to = paginations[index + 1];
                for (int left = from; left < to - 1; left++) {
                    for (int right = left + 1; right < to; right++) {
                        // TODO 注意:此处存在0的情况.
                        boolean change = false;
                        for (int dimension = 0, order = accessor.getQualityOrder(); dimension < order; dimension++) {
                            instance.setCursor(positions[left]);
                            int leftValue = instance.getQualityFeature(dimension);
                            instance.setCursor(positions[right]);
                            int rightValue = instance.getQualityFeature(dimension);
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
                        for (int dimension = 0, order = accessor.getQuantityOrder(); dimension < order; dimension++) {
                            instance.setCursor(positions[left]);
                            double leftValue = instance.getQuantityFeature(dimension);
                            instance.setCursor(positions[right]);
                            double rightValue = instance.getQuantityFeature(dimension);
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
    public static DataSorter discreteOf(DataModule accessor, int dimension) {
        DataInstance instance = accessor.getInstance(0);
        return (paginations, positions) -> {
            for (int index = 0, size = paginations.length - 1; index < size; index++) {
                int from = paginations[index], to = paginations[index + 1];
                for (int left = from; left < to; left++) {
                    for (int right = left + 1; right < to; right++) {
                        // TODO 注意:此处存在0的情况.
                        instance.setCursor(positions[left]);
                        int leftValue = instance.getQualityFeature(dimension);
                        instance.setCursor(positions[right]);
                        int rightValue = instance.getQualityFeature(dimension);
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
    public static DataSorter continuousOf(DataModule accessor, int dimension) {
        DataInstance instance = accessor.getInstance(0);
        return (paginations, positions) -> {
            for (int index = 0, size = paginations.length - 1; index < size; index++) {
                int from = paginations[index], to = paginations[index + 1];
                for (int left = from; left < to; left++) {
                    for (int right = left + 1; right < to; right++) {
                        // TODO 注意:此处存在0的情况.
                        instance.setCursor(positions[left]);
                        double leftValue = instance.getQuantityFeature(dimension);
                        instance.setCursor(positions[right]);
                        double rightValue = instance.getQuantityFeature(dimension);
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
