package com.jstarcraft.recommendation.data;

/**
 * 数据特征
 * 
 * <pre>
 * 每个数据特征对应一个数据属性,
 * </pre>
 * 
 * @author Birdy
 *
 * @param <T>
 */
@Deprecated
public interface DataFeature<T> extends Iterable<T> {

	/**
	 * 关联数据
	 * 
	 * @param data
	 */
	void associate(Object data);

	/**
	 * 获取属性
	 * 
	 * @return
	 */
	DataAttribute<?> getAttribute();

	/**
	 * 获取名称
	 * 
	 * @return
	 */
	String getName();

	/**
	 * 获取大小
	 * 
	 * @return
	 */
	int getSize();

}
