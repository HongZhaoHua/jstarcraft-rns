package com.jstarcraft.recommendation.data;

import com.jstarcraft.core.common.conversion.csv.ConversionUtility;

/**
 * 连续属性
 * 
 * @author Birdy
 *
 */
@Deprecated
public class ContinuousAttribute implements DataAttribute<Float> {

	/** 属性名称 */
	private String name;

	/** 属性类型 */
	private Class<Float> type;

	private float maximum;

	private float minimum;

	ContinuousAttribute(String name) {
		this.name = name;
		this.type = Float.class;
		this.maximum = Float.NEGATIVE_INFINITY;
		this.minimum = Float.POSITIVE_INFINITY;
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
	public Float makeValue(Object data) {
		Float feature = ConversionUtility.convert(data, type);
		if (feature > maximum) {
			maximum = feature;
		}
		if (feature < minimum) {
			minimum = feature;
		}
		return feature;
	}

	@Override
	public Object[] getDatas() {
		return new Object[] { minimum, maximum };
	}

}
