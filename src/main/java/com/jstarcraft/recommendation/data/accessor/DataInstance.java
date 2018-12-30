package com.jstarcraft.recommendation.data.accessor;

/**
 * 数据实例
 * 
 * @author Birdy
 *
 */
@Deprecated
public class DataInstance {

	/** 游标 */
	private Integer cursor;

	/** 离散特征 */
	private int[][] discreteFeatures;

	/** 连续特征 */
	private float[][] continuousFeatures;

	DataInstance(int[][] discreteFeatures, float[][] continuousFeatures) {
		this.discreteFeatures = discreteFeatures;
		this.continuousFeatures = continuousFeatures;
	}

	void update(int cursor) {
		this.cursor = cursor;
	}

	/**
	 * 获取离散特征
	 * 
	 * @param dimension
	 * @return
	 */
	public int getDiscreteFeature(int dimension) {
		return discreteFeatures[dimension][cursor];
	}

	/**
	 * 获取连续特征
	 * 
	 * @param dimension
	 * @return
	 */
	public float getContinuousFeature(int dimension) {
		return continuousFeatures[dimension][cursor];
	}

}
