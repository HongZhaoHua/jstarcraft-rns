package com.jstarcraft.rns.utility;

import com.jstarcraft.ai.math.structure.vector.MathVector;

/**
 * 采样工具
 * 
 * @author Birdy
 *
 */
public class SampleUtility {

    /**
     * 二分查找
     * 
     * @param values
     * @param low
     * @param high
     * @param random
     * @return
     */
    public static int binarySearch(float[] values, int low, int high, float random) {
        while (low < high) {
            if (random < values[low]) {
                return low;
            }
            if (random >= values[high - 1]) {
                return high;
            }
            int middle = (low + high) / 2;
            if (random < values[middle]) {
                high = middle;
            } else {
                low = middle;
            }
        }
        // throw new RecommendationException("概率范围超过随机范围,检查是否由于多线程修改导致.");
        return -1;
    }

    /**
     * 二分查找
     * 
     * @param vector
     * @param low
     * @param high
     * @param random
     * @return
     */
    public static int binarySearch(MathVector vector, int low, int high, float random) {
        while (low < high) {
            if (random < vector.getValue(low)) {
                return low;
            }
            if (random >= vector.getValue(high - 1)) {
                return high;
            }
            int middle = (low + high) / 2;
            if (random < vector.getValue(middle)) {
                high = middle;
            } else {
                low = middle;
            }
        }
        // throw new RecommendationException("概率范围超过随机范围,检查是否由于多线程修改导致.");
        return -1;
    }

}
