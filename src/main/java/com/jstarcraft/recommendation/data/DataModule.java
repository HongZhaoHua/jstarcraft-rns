package com.jstarcraft.recommendation.data;

import java.util.Collection;

import com.jstarcraft.ai.data.attribute.QualityAttribute;
import com.jstarcraft.ai.data.attribute.QuantityAttribute;

/**
 * 数据访问器
 * 
 * @author Birdy
 *
 */
// TODO 考虑改名为DataModule
public interface DataModule<T> extends Iterable<T> {

	/**
	 * 获取指定维度的离散属性
	 * 
	 * @param dimension
	 * @return
	 */
	public QualityAttribute getQualityAttribute(int dimension);

	/**
	 * 获取指定维度的连续属性
	 * 
	 * @param dimension
	 * @return
	 */
	public QuantityAttribute getQuantityAttribute(int dimension);

	/**
	 * 获取指定离散字段的维度
	 * 
	 * @param field
	 * @return
	 */
	public Integer getQualityInner(String field);

	/**
	 * 获取指定连续字段的维度
	 * 
	 * @param field
	 * @return
	 */
	public Integer getQuantityInner(String field);

	/**
	 * 获取指定维度与位置的离散特征
	 * 
	 * @param dimension
	 * @param position
	 * @return
	 */
	public int getQualityFeature(int dimension, int position);

	/**
	 * 获取指定维度与位置的连续特征
	 * 
	 * @param dimension
	 * @param position
	 * @return
	 */
	public float getQuantityFeature(int dimension, int position);

	/**
	 * 获取离散字段(名称)
	 * 
	 * @return
	 */
	public Collection<String> getQualityFields();

	/**
	 * 获取连续字段(名称)
	 * 
	 * @return
	 */
	public Collection<String> getQuantityFields();

	/**
	 * 获取稀疏秩的大小
	 * 
	 * @return
	 */
	int getQualityOrder();

	/**
	 * 获取连续秩的大小
	 * 
	 * @return
	 */
	int getQuantityOrder();

	/**
	 * 获取大小
	 * 
	 * @return
	 */
	public int getSize();

}
