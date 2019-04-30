package com.jstarcraft.recommendation.data.accessor;

import com.jstarcraft.ai.utility.FloatArray;
import com.jstarcraft.ai.utility.IntegerArray;

/**
 * 数据样本
 * 
 * @author Birdy
 *
 */
public class DataSample {

	/** 游标 */
	private Integer position;

	/** 离散特征 */
	private IntegerArray[] discreteFeatures;

	/** 连续特征 */
	private FloatArray[] continuousFeatures;

	private float mark;

	DataSample(IntegerArray[] discreteFeatures, FloatArray[] continuousFeatures) {
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
		return discreteFeatures[dimension].getData(position);
	}

	/**
	 * 获取连续特征
	 * 
	 * @param dimension
	 * @return
	 */
	public float getContinuousFeature(int dimension) {
		return continuousFeatures[dimension].getData(position);
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
