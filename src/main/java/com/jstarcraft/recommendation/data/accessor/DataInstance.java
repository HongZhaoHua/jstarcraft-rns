package com.jstarcraft.recommendation.data.accessor;

import com.jstarcraft.ai.utility.FloatArray;
import com.jstarcraft.ai.utility.IntegerArray;

/**
 * 数据实例
 * 
 * @author Birdy
 *
 */
public class DataInstance {

	/** 游标 */
	private int cursor;

	/** 离散特征 */
	private IntegerArray[] discreteFeatures;

	/** 连续特征 */
	private FloatArray[] continuousFeatures;

	DataInstance(int cursor, DenseModule module) {
		this.discreteFeatures = module.getQualityValues();
		this.continuousFeatures = module.getQuantityValues();
	}

	void setCursor(int cursor) {
		this.cursor = cursor;
	}

	/**
	 * 获取离散特征
	 * 
	 * @param dimension
	 * @return
	 */
	public int getQualityFeature(int dimension) {
		return discreteFeatures[dimension].getData(cursor);
	}

	/**
	 * 获取连续特征
	 * 
	 * @param dimension
	 * @return
	 */
	public float getQuantityFeature(int dimension) {
		return continuousFeatures[dimension].getData(cursor);
	}

}
