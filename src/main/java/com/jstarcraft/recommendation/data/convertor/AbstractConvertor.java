package com.jstarcraft.recommendation.data.convertor;

import java.util.Map;

public abstract class AbstractConvertor<T> implements DataConvertor {

	/** 名称 */
	protected final String name;

	/** 字段 */
	protected final Map<String, T> fields;

	protected AbstractConvertor(String name, Map<String, T> fields) {
		this.name = name;
		this.fields = fields;
	}

	@Override
	public String getName() {
		return name;
	}

}
