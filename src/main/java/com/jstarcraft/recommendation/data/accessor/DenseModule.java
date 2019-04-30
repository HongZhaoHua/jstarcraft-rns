package com.jstarcraft.recommendation.data.accessor;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.jstarcraft.ai.data.DataAttribute;
import com.jstarcraft.ai.data.attribute.QualityAttribute;
import com.jstarcraft.ai.data.attribute.QuantityAttribute;
import com.jstarcraft.recommendation.data.DataModule;
import com.jstarcraft.recommendation.data.QualityFeature;
import com.jstarcraft.recommendation.data.QuantityFeature;

/**
 * 数据模型
 * 
 * <pre>
 * 负责数据实例管理(分组与排序)
 * 配合{@link DataStorage},{@link DataAttribute}与{@link DataModule}实现数据管理.
 * </pre>
 * 
 * @author Birdy
 *
 */
public class DenseModule implements DataModule<DataInstance> {

	/** 离散属性 */
	QualityAttribute[] qualityAttributes;

	/** 连续属性 */
	QuantityAttribute[] quantityAttributes;

	/** 离散特征 */
	int[][] qualityFeatures;

	/** 连续特征 */
	float[][] quantityFeatures;

	/** 离散维度 */
	private Map<String, Integer> qualityDimensions;

	/** 连续维度 */
	private Map<String, Integer> quantityDimensions;

	/** 大小 */
	private int size = 0;

	public DenseModule(List<QualityFeature> qualityFeatures, List<QuantityFeature> quantityFeatures) {
		this.qualityAttributes = new QualityAttribute[qualityFeatures.size()];
		this.quantityAttributes = new QuantityAttribute[quantityFeatures.size()];
		this.qualityFeatures = new int[qualityFeatures.size()][];
		this.quantityFeatures = new float[quantityFeatures.size()][];
		this.qualityDimensions = new LinkedHashMap<>();
		this.quantityDimensions = new LinkedHashMap<>();
		this.size = qualityFeatures.get(0).getSize();
		for (int index = 0; index < qualityFeatures.size(); index++) {
			QualityFeature feature = qualityFeatures.get(index);
			if (feature.getSize() != this.size) {
				throw new IllegalArgumentException("特征大小不一致");
			}
			int[] data = new int[this.size];
			int cursor = 0;
			for (int value : feature) {
				data[cursor++] = value;
			}
			this.qualityAttributes[index] = feature.getAttribute();
			this.qualityFeatures[index] = data;
			this.qualityDimensions.put(feature.getName(), index);
		}
		for (int index = 0; index < quantityFeatures.size(); index++) {
			QuantityFeature feature = quantityFeatures.get(index);
			if (feature.getSize() != this.size) {
				throw new IllegalArgumentException("特征大小不一致");
			}
			float[] data = new float[this.size];
			int cursor = 0;
			for (float value : feature) {
				data[cursor++] = value;
			}
			this.quantityAttributes[index] = feature.getAttribute();
			this.quantityFeatures[index] = data;
			this.quantityDimensions.put(feature.getName(), index);
		}
	}

	// /**
	// * 制作实例
	// *
	// * @param values
	// * @return
	// */
	// public int makeFeatures(String... values) {
	// if (discreteAttributes.size() + continuousAttributes.size() !=
	// values.length) {
	// throw new IllegalArgumentException();
	// }
	// int position = size % capacity;
	// if (position == 0) {
	// discreteFeatures = new int[discreteAttributes.size()][capacity];
	// discreteDatas.add(discreteFeatures);
	// continuousFeatures = new double[continuousAttributes.size()][capacity];
	// continuousDatas.add(continuousFeatures);
	// }
	// for (int dimension = 0; dimension < discreteAttributes.size();
	// dimension++) {
	// KeyValue<DiscreteAttribute, Integer> keyValue =
	// discreteAttributes.get(dimension);
	// DiscreteAttribute discreteAttribute = keyValue.getKey();
	// String value = values[keyValue.getValue()];
	// discreteFeatures[dimension][position] =
	// discreteAttribute.makeFeature(value);
	// }
	// for (int dimension = 0; dimension < continuousAttributes.size();
	// dimension++) {
	// KeyValue<ContinuousAttribute, Integer> keyValue =
	// continuousAttributes.get(dimension);
	// ContinuousAttribute continuousAttribute = keyValue.getKey();
	// String value = values[keyValue.getValue()];
	// continuousFeatures[dimension][position] =
	// continuousAttribute.makeFeature(value);
	// }
	// return size++;
	// }
	//
	// /**
	// * 获取数据属性
	// *
	// * @return
	// */
	// public List<DataAttribute<?>> getDataAttributes() {
	// int size = discreteAttributes.size() + continuousAttributes.size();
	// List<DataAttribute<?>> attributes = new ArrayList<>(size);
	// for (KeyValue<DiscreteAttribute, Integer> keyValue : discreteAttributes)
	// {
	// attributes.add(keyValue.getKey());
	// }
	// for (KeyValue<ContinuousAttribute, Integer> keyValue :
	// continuousAttributes) {
	// attributes.add(keyValue.getKey());
	// }
	// return attributes;
	// }
	//
	// /**
	// * 获取离散属性
	// *
	// * @return
	// */
	// public List<DiscreteAttribute> getDiscreteAttributes() {
	// int size = discreteAttributes.size();
	// List<DiscreteAttribute> attributes = new ArrayList<>(size);
	// for (KeyValue<DiscreteAttribute, Integer> keyValue : discreteAttributes)
	// {
	// attributes.add(keyValue.getKey());
	// }
	// return attributes;
	// }
	//
	// /**
	// * 获取连续属性
	// *
	// * @return
	// */
	// public List<ContinuousAttribute> getContinuousAttributes() {
	// int size = continuousAttributes.size();
	// List<ContinuousAttribute> attributes = new ArrayList<>(size);
	// for (KeyValue<ContinuousAttribute, Integer> keyValue :
	// continuousAttributes) {
	// attributes.add(keyValue.getKey());
	// }
	// return attributes;
	// }

	@Override
	public QualityAttribute getQualityAttribute(int dimension) {
		return qualityAttributes[dimension];
	}

	@Override
	public QuantityAttribute getQuantityAttribute(int dimension) {
		return quantityAttributes[dimension];
	}

	@Override
	public Integer getQualityDimension(String name) {
		return qualityDimensions.get(name);
	}

	@Override
	public Integer getQuantityDimension(String name) {
		return quantityDimensions.get(name);
	}

	@Override
	public int getQualityFeature(int dimension, int position) {
		return qualityFeatures[dimension][position];
	}

	@Override
	public float getQuantityFeature(int dimension, int position) {
		return quantityFeatures[dimension][position];
	}

	@Override
	public Collection<String> getQualityFields() {
		return qualityDimensions.keySet();
	}

	@Override
	public Collection<String> getQuantityFields() {
		return quantityDimensions.keySet();
	}

	@Override
	public int getQualityOrder() {
		return qualityAttributes.length;
	}

	@Override
	public int getQuantityOrder() {
		return quantityAttributes.length;
	}

	@Override
	public int getSize() {
		return size;
	}

	// /**
	// * 分组数据实例
	// *
	// * @param dimension
	// * @return
	// */
	// @Deprecated
	// public int[][] groupInstances(int dimension) {
	// DiscreteAttribute attribute = discreteAttributes.get(dimension).getKey();
	// int[] counts = new int[attribute.getSize()];
	// int[] sizes = new int[attribute.getSize()];
	// int[][] groups = new int[attribute.getSize()][];
	// int cursor = 0;
	// for (int[][] discreteFeatures : discreteDatas) {
	// int[] dimensionFeatures = discreteFeatures[dimension];
	// for (int feature : dimensionFeatures) {
	// if (cursor < size) {
	// sizes[feature]++;
	// cursor++;
	// } else {
	// break;
	// }
	// }
	// }
	// cursor = 0;
	// for (int[][] discreteFeatures : discreteDatas) {
	// int[] dimensionFeatures = discreteFeatures[dimension];
	// for (int feature : dimensionFeatures) {
	// if (cursor < size) {
	// int[] group = groups[feature];
	// if (group == null) {
	// group = new int[sizes[feature]];
	// groups[feature] = group;
	// }
	// group[counts[feature]++] = cursor++;
	// } else {
	// break;
	// }
	// }
	// }
	// return groups;
	// }

	@Override
	public Iterator<DataInstance> iterator() {
		return new DataInstanceIterator();
	}

	private class DataInstanceIterator implements Iterator<DataInstance> {

		private int cursor = 0;

		private DataInstance instance = new DataInstance(qualityFeatures, quantityFeatures);

		@Override
		public boolean hasNext() {
			return cursor < size;
		}

		@Override
		public DataInstance next() {
			instance.update(cursor++);
			return instance;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	// /**
	// * 排序数据实例
	// *
	// * @param comparator
	// * @return
	// */
	// public int[] sortInstances(DataSorter sorter) {
	// int[] sorts = new int[size];
	// for (int index = 0; index < size; index++) {
	// sorts[index++] = index;
	// }
	// sorter.sort(sorts);
	// return sorts;
	// }
	//
	// @Override
	// public Iterator<DataInstance> iterator() {
	// return new DataInstanceIterator();
	// }
	//
	// private class DataInstanceIterator implements Iterator<DataInstance> {
	//
	// private int cursor = 0;
	//
	// private DataInstance[] current;
	//
	// private final Iterator<DataInstance[]> iterator = instances.iterator();
	//
	// @Override
	// public boolean hasNext() {
	// return cursor < size;
	// }
	//
	// @Override
	// public DataInstance next() {
	// int position = cursor++ % capacity;
	// if (position == 0) {
	// current = iterator.next();
	// }
	// return current[position];
	// }
	//
	// @Override
	// public void remove() {
	// throw new UnsupportedOperationException();
	// }
	//
	// }

}
