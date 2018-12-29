package com.jstarcraft.recommendation.data;

/**
 * 数据属性
 * 
 * @author Birdy
 *
 */
@Deprecated
public interface DataAttribute<T> {

	/**
	 * 获取属性名称
	 * 
	 * @return
	 */
	String getName();

	/**
	 * 获取属性类型
	 * 
	 * @return
	 */
	Class<?> getType();

	/**
	 * 制作属性值
	 * 
	 * @param value
	 * @return
	 */
	T makeValue(Object value);

	/**
	 * 获取属性数据
	 * 
	 * @return
	 */
	Object[] getDatas();

}
