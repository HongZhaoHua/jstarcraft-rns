package com.jstarcraft.recommendation.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.jstarcraft.recommendation.data.accessor.InstanceAccessor;

/**
 * 数据空间
 * 
 * <pre>
 * 配合{@link DataAttribute},{@link DataFeature}与{@link InstanceAccessor}实现数据管理.
 * </pre>
 * 
 * @author Birdy
 *
 */
public class DataSpace {

	/** 离散属性映射 */
	private Map<String, QualityAttribute> discreteAttributes = new HashMap<>();

	/** 连续属性映射 */
	private Map<String, QuantityAttribute> continuousAttributes = new HashMap<>();

	/** 特征映射 */
	private Map<String, DataFeature<?>> features = new HashMap<>();

	/** 模型映射 */
	private Map<String, InstanceAccessor> modules = new HashMap<>();

	public DataSpace(Map<String, Class<?>> discreteDifinitions, Set<String> continuousDifinitions) {
		for (Entry<String, Class<?>> keyValue : discreteDifinitions.entrySet()) {
			if (continuousAttributes.containsKey(keyValue.getKey())) {
				throw new IllegalArgumentException("属性冲突");
			}
			QualityAttribute attribute = new QualityAttribute(keyValue.getKey(), keyValue.getValue());
			discreteAttributes.put(attribute.getName(), attribute);
		}
		for (String feature : continuousDifinitions) {
			if (discreteAttributes.containsKey(feature)) {
				throw new IllegalArgumentException("属性冲突");
			}
			QuantityAttribute attribute = new QuantityAttribute(feature);
			continuousAttributes.put(attribute.getName(), attribute);
		}
	}

	public QualityAttribute getDiscreteAttribute(String attributeName) {
		return discreteAttributes.get(attributeName);
	}

	public QuantityAttribute getContinuousAttribute(String attributeName) {
		return continuousAttributes.get(attributeName);
	}

	/**
	 * 制作数据特征
	 * 
	 * @param featureName
	 * @param attributeName
	 * @return
	 */
	public DataFeature<?> makeFeature(String featureName, String attributeName) {
		if (discreteAttributes.containsKey(attributeName)) {
			QualityAttribute attribute = discreteAttributes.get(attributeName);
			DataFeature<?> feature = new QualityFeature(featureName, attribute);
			features.put(featureName, feature);
			return feature;
		}
		if (continuousAttributes.containsKey(attributeName)) {
			QuantityAttribute attribute = continuousAttributes.get(attributeName);
			DataFeature<?> feature = new QuantityFeature(featureName, attribute);
			features.put(featureName, feature);
			return feature;
		}
		throw new IllegalArgumentException("属性缺失");
	}

	public DataFeature<?> getFeature(String featureName) {
		DataFeature<?> feature = features.get(featureName);
		return feature;
	}

	/**
	 * 制作数据模块
	 * 
	 * @param moduleName
	 * @param featureNames
	 * @return
	 */
	public InstanceAccessor makeModule(String moduleName, String... featureNames) {
		InstanceAccessor model = modules.get(moduleName);
		if (model == null) {
			LinkedList<QualityFeature> discreteProperties = new LinkedList<>();
			LinkedList<QuantityFeature> continuousProperties = new LinkedList<>();
			for (String featureName : featureNames) {
				DataFeature<?> feature = features.get(featureName);
				if (feature instanceof QualityFeature) {
					discreteProperties.add(QualityFeature.class.cast(feature));
					continue;
				}
				if (feature instanceof QuantityFeature) {
					continuousProperties.add(QuantityFeature.class.cast(feature));
					continue;
				}
				throw new IllegalArgumentException("特征缺失");
			}
			model = new InstanceAccessor(new ArrayList<>(discreteProperties), new ArrayList<>(continuousProperties));
			modules.put(moduleName, model);
			return model;
		} else {
			throw new IllegalArgumentException("属性冲突");
		}
	}

	/**
	 * 获取数据模块
	 * 
	 * @param moduleName
	 * @return
	 */
	public InstanceAccessor getModule(String moduleName) {
		return modules.get(moduleName);
	}

}
