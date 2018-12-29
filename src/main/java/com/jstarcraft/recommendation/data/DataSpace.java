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
	private Map<String, DiscreteAttribute> discreteAttributes = new HashMap<>();

	/** 连续属性映射 */
	private Map<String, ContinuousAttribute> continuousAttributes = new HashMap<>();

	/** 特征映射 */
	private Map<String, DataFeature<?>> features = new HashMap<>();

	/** 模型映射 */
	private Map<String, InstanceAccessor> modules = new HashMap<>();

	public DataSpace(Map<String, Class<?>> discreteDifinitions, Set<String> continuousDifinitions) {
		for (Entry<String, Class<?>> keyValue : discreteDifinitions.entrySet()) {
			if (continuousAttributes.containsKey(keyValue.getKey())) {
				throw new IllegalArgumentException("属性冲突");
			}
			DiscreteAttribute attribute = new DiscreteAttribute(keyValue.getKey(), keyValue.getValue());
			discreteAttributes.put(attribute.getName(), attribute);
		}
		for (String feature : continuousDifinitions) {
			if (discreteAttributes.containsKey(feature)) {
				throw new IllegalArgumentException("属性冲突");
			}
			ContinuousAttribute attribute = new ContinuousAttribute(feature);
			continuousAttributes.put(attribute.getName(), attribute);
		}
	}

	public DiscreteAttribute getDiscreteAttribute(String attributeName) {
		return discreteAttributes.get(attributeName);
	}

	public ContinuousAttribute getContinuousAttribute(String attributeName) {
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
			DiscreteAttribute attribute = discreteAttributes.get(attributeName);
			DataFeature<?> feature = new DiscreteFeature(featureName, attribute);
			features.put(featureName, feature);
			return feature;
		}
		if (continuousAttributes.containsKey(attributeName)) {
			ContinuousAttribute attribute = continuousAttributes.get(attributeName);
			DataFeature<?> feature = new ContinuousFeature(featureName, attribute);
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
			LinkedList<DiscreteFeature> discreteProperties = new LinkedList<>();
			LinkedList<ContinuousFeature> continuousProperties = new LinkedList<>();
			for (String featureName : featureNames) {
				DataFeature<?> feature = features.get(featureName);
				if (feature instanceof DiscreteFeature) {
					discreteProperties.add(DiscreteFeature.class.cast(feature));
					continue;
				}
				if (feature instanceof ContinuousFeature) {
					continuousProperties.add(ContinuousFeature.class.cast(feature));
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
