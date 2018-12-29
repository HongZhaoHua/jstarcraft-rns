package com.jstarcraft.recommendation.data.accessor;

/**
 * 数据样本
 * 
 * @author Birdy
 *
 */
@Deprecated
public class DataSample {

	/** 游标 */
	private Integer position;

	/** 离散特征 */
	private int[][] discreteFeatures;

	/** 连续特征 */
	private float[][] continuousFeatures;

	private float mark;

	DataSample(int[][] discreteFeatures, float[][] continuousFeatures) {
		this.discreteFeatures = discreteFeatures;
		this.continuousFeatures = continuousFeatures;
	}

	void update(int position, float mark) {
		this.position = position;
		this.mark = mark;
	}

	/**
	 * 获取离散特征
	 * 
	 * @param dimension
	 * @return
	 */
	public int getDiscreteFeature(int dimension) {
		return discreteFeatures[dimension][position];
	}

	/**
	 * 获取连续特征
	 * 
	 * @param dimension
	 * @return
	 */
	public float getContinuousFeature(int dimension) {
		return continuousFeatures[dimension][position];
	}

	/**
	 * 获取标记
	 * 
	 * @return
	 */
	public float getMark() {
		return mark;
	}

}
