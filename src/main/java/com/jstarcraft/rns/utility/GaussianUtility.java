package com.jstarcraft.rns.utility;

import com.jstarcraft.ai.math.MathUtility;

/**
 * 高斯工具
 * 
 * <pre>
 * https://en.wikipedia.org/wiki/Normal_distribution
 * http://www.cnblogs.com/mengfanrong/p/4369545.html
 * </pre>
 * 
 * @author Birdy
 *
 */
public class GaussianUtility {

	public static float probabilityDensity(float value) {
		return (float) (Math.exp(-0.5F * value * value) / Math.sqrt(2F * Math.PI));
	}

	public static float probabilityDensity(float value, float mean, float standardDeviation) {
		return probabilityDensity((value - mean) / standardDeviation) / standardDeviation;
	}

	public static float cumulativeDistribution(float value) {
		if (value < -8F) {
			return 0F;
		}
		if (value > 8F) {
			return 1F;
		}
		float sum = 0F, term = value;
		for (int index = 3; sum + term != sum; index += 2) {
			sum = sum + term;
			term = term * value * value / index;
		}
		return 0.5F + sum * probabilityDensity(value);
	}

	public static float cumulativeDistribution(float value, float mean, float standardDeviation) {
		return cumulativeDistribution((value - mean) / standardDeviation);
	}

	public static float inverseDistribution(float value) {
		return inverseDistribution(value, MathUtility.EPSILON, -8F, 8F);
	}

	private static float inverseDistribution(float value, float delta, float minimum, float maximum) {
		float median = minimum + (maximum - minimum) / 2F;
		if (maximum - minimum < delta) {
			return median;
		}
		if (cumulativeDistribution(median) > value) {
			return inverseDistribution(value, delta, minimum, median);
		} else {
			return inverseDistribution(value, delta, median, maximum);
		}
	}

	public static float inverseDistribution(float value, float mean, float standardDeviation) {
		return inverseDistribution(value, mean, standardDeviation, MathUtility.EPSILON, (mean - 8F * standardDeviation), (mean + 8F * standardDeviation));
	}

	private static float inverseDistribution(float value, float mean, float standardDeviation, float delta, float minimum, float maximum) {
		float median = minimum + (maximum - minimum) / 2F;
		if (maximum - minimum < delta) {
			return median;
		}
		if (cumulativeDistribution(median, mean, standardDeviation) > value) {
			return inverseDistribution(value, mean, standardDeviation, delta, minimum, median);
		} else {
			return inverseDistribution(value, mean, standardDeviation, delta, median, maximum);
		}
	}

}
