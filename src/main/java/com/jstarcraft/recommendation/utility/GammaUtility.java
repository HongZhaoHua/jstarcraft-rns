package com.jstarcraft.recommendation.utility;

/**
 * 伽玛工具
 * 
 * @author Birdy
 *
 */
public class GammaUtility {

	/**
	 * 伽玛函数
	 * 
	 * @param value
	 * @return
	 */
	public static float gamma(float value) {
		return (float) Math.exp(logGamma(value));
	}

	/**
	 * 伽玛函数的对数
	 * 
	 * @param value
	 * @return
	 */
	public static float logGamma(float value) {
		if (value <= 0F) {
			return Float.NaN;
		}
		float tmp = (float) ((value - 0.5F) * Math.log(value + 4.5F) - (value + 4.5F));
		float ser = 1F + 76.18009173F / (value + 0F) - 86.50532033F / (value + 1F) + 24.01409822F / (value + 2F) - 1.231739516F / (value + 3F) + 0.00120858003F / (value + 4F) - 0.00000536382F / (value + 5F);
		return (float) (tmp + Math.log(ser * Math.sqrt(2F * Math.PI)));
	}

	/**
	 * 伽玛函数的对数的一阶导数
	 * 
	 * @param value
	 * @return
	 */
	public static float digamma(float value) {
		return Digamma.calculate(value);
	}

	/**
	 * 伽玛函数的对数的二阶导数
	 * 
	 * @param value
	 * @return
	 */
	public static float trigamma(float value) {
		return Trigamma.calculate(value);
	}

	public static float inverse(float y, int n) {
		return Digamma.inverse(y, n);
	}

}
