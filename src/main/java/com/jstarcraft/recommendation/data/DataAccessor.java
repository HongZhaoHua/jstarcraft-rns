package com.jstarcraft.recommendation.data;

import java.util.Collection;

/**
 * 数据访问器
 * 
 * @author Birdy
 *
 */
// TODO 考虑改名为DataModule
public interface DataAccessor<T> extends Iterable<T> {

	/**
	 * 获取指定维度的离散属性
	 * 
	 * @param dimension
	 * @return
	 */
	public DiscreteAttribute getDiscreteAttribute(int dimension);

	/**
	 * 获取指定维度的连续属性
	 * 
	 * @param dimension
	 * @return
	 */
	public ContinuousAttribute getContinuousAttribute(int dimension);

	/**
	 * 获取指定离散字段的维度
	 * 
	 * @param field
	 * @return
	 */
	public Integer getDiscreteDimension(String field);

	/**
	 * 获取指定连续字段的维度
	 * 
	 * @param field
	 * @return
	 */
	public Integer getContinuousDimension(String field);

	/**
	 * 获取指定维度与位置的离散特征
	 * 
	 * @param dimension
	 * @param position
	 * @return
	 */
	public int getDiscreteFeature(int dimension, int position);

	/**
	 * 获取指定维度与位置的连续特征
	 * 
	 * @param dimension
	 * @param position
	 * @return
	 */
	public float getContinuousFeature(int dimension, int position);

	/**
	 * 获取离散字段(名称)
	 * 
	 * @return
	 */
	public Collection<String> getDiscreteFields();

	/**
	 * 获取连续字段(名称)
	 * 
	 * @return
	 */
	public Collection<String> getContinuousFields();

	/**
	 * 获取稀疏秩的大小
	 * 
	 * @return
	 */
	int getDiscreteOrder();

	/**
	 * 获取连续秩的大小
	 * 
	 * @return
	 */
	int getContinuousOrder();

	/**
	 * 获取大小
	 * 
	 * @return
	 */
	public int getSize();

}
