package com.jstarcraft.recommendation.data;

import java.util.HashMap;
import java.util.Map.Entry;

import com.jstarcraft.core.common.conversion.csv.ConversionUtility;

/**
 * 离散属性
 * 
 * @author Birdy
 *
 */
// TODO 准备实现支持分布式的indexes
public class QualityAttribute implements DataAttribute<Integer> {

	/** 属性名称 */
	private String name;

	/** 属性类型 */
	private Class<?> type;

	/** 外部键-内部索引映射 */
	private HashMap<Object, Integer> indexes = new HashMap<>();

	QualityAttribute(String name, Class<?> type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<?> getType() {
		return type;
	}

	@Override
	public Integer makeValue(Object data) {
		Object object = ConversionUtility.convert(data, type);
		Integer index = indexes.get(object);
		if (index == null) {
			index = indexes.size();
			indexes.put(object, index);
		}
		return index;
	}

	@Override
	public Object[] getDatas() {
		Object[] keys = new Object[indexes.size()];
		for (Entry<Object, Integer> term : indexes.entrySet()) {
			keys[term.getValue()] = term.getKey();
		}
		return keys;
	}

	public int getSize() {
		return indexes.size();
	}

}
