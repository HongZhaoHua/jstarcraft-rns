package com.jstarcraft.recommendation.data.accessor;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.jstarcraft.recommendation.data.QuantityAttribute;
import com.jstarcraft.recommendation.data.QuantityFeature;
import com.jstarcraft.recommendation.data.DataAccessor;
import com.jstarcraft.recommendation.data.DataAttribute;
import com.jstarcraft.recommendation.data.QualityAttribute;
import com.jstarcraft.recommendation.data.QualityFeature;

/**
 * 数据模型
 * 
 * <pre>
 * 负责数据实例管理(分组与排序)
 * 配合{@link DataStorage},{@link DataAttribute}与{@link DataAccessor}实现数据管理.
 * </pre>
 * 
 * @author Birdy
 *
 */
public class InstanceAccessor implements DataAccessor<DataInstance> {

	/** 离散属性 */
	QualityAttribute[] discreteAttributes;

	/** 连续属性 */
	QuantityAttribute[] continuousAttributes;

	/** 离散特征 */
	int[][] discreteFeatures;

	/** 连续特征 */
	float[][] continuousFeatures;

	/** 离散维度 */
	private Map<String, Integer> discreteDimensions;

	/** 连续维度 */
	private Map<String, Integer> continuousDimensions;

	/** 大小 */
	private int size = 0;

	public InstanceAccessor(List<QualityFeature> discreteFeatures, List<QuantityFeature> continuousFeatures) {
		this.discreteAttributes = new QualityAttribute[discreteFeatures.size()];
		this.continuousAttributes = new QuantityAttribute[continuousFeatures.size()];
		this.discreteFeatures = new int[discreteFeatures.size()][];
		this.continuousFeatures = new float[continuousFeatures.size()][];
		this.discreteDimensions = new LinkedHashMap<>();
		this.continuousDimensions = new LinkedHashMap<>();
		this.size = discreteFeatures.get(0).getSize();
		for (int index = 0; index < discreteFeatures.size(); index++) {
			QualityFeature feature = discreteFeatures.get(index);
			if (feature.getSize() != this.size) {
				throw new IllegalArgumentException("特征大小不一致");
			}
			int[] data = new int[this.size];
			int cursor = 0;
			for (int value : feature) {
				data[cursor++] = value;
			}
			this.discreteAttributes[index] = feature.getAttribute();
			this.discreteFeatures[index] = data;
			this.discreteDimensions.put(feature.getName(), index);
		}
		for (int index = 0; index < continuousFeatures.size(); index++) {
			QuantityFeature feature = continuousFeatures.get(index);
			if (feature.getSize() != this.size) {
				throw new IllegalArgumentException("特征大小不一致");
			}
			float[] data = new float[this.size];
			int cursor = 0;
			for (float value : feature) {
				data[cursor++] = value;
			}
			this.continuousAttributes[index] = feature.getAttribute();
			this.continuousFeatures[index] = data;
			this.continuousDimensions.put(feature.getName(), index);
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
	public QualityAttribute getDiscreteAttribute(int dimension) {
		return discreteAttributes[dimension];
	}

	@Override
	public QuantityAttribute getContinuousAttribute(int dimension) {
		return continuousAttributes[dimension];
	}

	@Override
	public Integer getDiscreteDimension(String name) {
		return discreteDimensions.get(name);
	}

	@Override
	public Integer getContinuousDimension(String name) {
		return continuousDimensions.get(name);
	}

	@Override
	public int getDiscreteFeature(int dimension, int position) {
		return discreteFeatures[dimension][position];
	}

	@Override
	public float getContinuousFeature(int dimension, int position) {
		return continuousFeatures[dimension][position];
	}

	@Override
	public Collection<String> getDiscreteFields() {
		return discreteDimensions.keySet();
	}

	@Override
	public Collection<String> getContinuousFields() {
		return continuousDimensions.keySet();
	}

	@Override
	public int getDiscreteOrder() {
		return discreteAttributes.length;
	}

	@Override
	public int getContinuousOrder() {
		return continuousAttributes.length;
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

		private DataInstance instance = new DataInstance(discreteFeatures, continuousFeatures);

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
