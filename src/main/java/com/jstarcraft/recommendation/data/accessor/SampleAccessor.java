package com.jstarcraft.recommendation.data.accessor;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.jstarcraft.ai.data.attribute.QualityAttribute;
import com.jstarcraft.ai.data.attribute.QuantityAttribute;
import com.jstarcraft.ai.utility.IntegerArray;
import com.jstarcraft.recommendation.data.DataModule;

/**
 * 数据标记器
 * 
 * @author Birdy
 *
 */
public abstract class SampleAccessor implements DataModule<DataSample> {

	/** 离散属性 */
	protected QualityAttribute[] discreteAttributes;

	/** 连续属性 */
	protected QuantityAttribute[] continuousAttributes;

	/** 离散特征 */
	protected int[][] discreteFeatures;

	/** 连续特征 */
	protected float[][] continuousFeatures;

	/** 离散维度 */
	protected Map<String, Integer> discreteDimensions;

	/** 连续维度 */
	protected Map<String, Integer> continuousDimensions;

	/** 位置 */
	protected IntegerArray positions;

	@Override
	public QualityAttribute getQualityAttribute(int dimension) {
		return discreteAttributes[dimension];
	}

	@Override
	public QuantityAttribute getQuantityAttribute(int dimension) {
		return continuousAttributes[dimension];
	}

	@Override
	public Integer getQualityDimension(String name) {
		return discreteDimensions.get(name);
	}

	@Override
	public Integer getQuantityDimension(String name) {
		return continuousDimensions.get(name);
	}

	@Override
	public int getQualityFeature(int dimension, int position) {
		return discreteFeatures[dimension][positions.getData(position)];
	}

	@Override
	public float getQuantityFeature(int dimension, int position) {
		return continuousFeatures[dimension][positions.getData(position)];
	}

	@Override
	public Collection<String> getQualityFields() {
		return discreteDimensions.keySet();
	}

	@Override
	public Collection<String> getQuantityFields() {
		return continuousDimensions.keySet();
	}

	@Override
	public int getQualityOrder() {
		return discreteAttributes.length;
	}

	@Override
	public int getQuantityOrder() {
		return continuousAttributes.length;
	}

	@Override
	public int getSize() {
		return positions.getSize();
	}

	public abstract float getMark(int position);

	@Override
	public Iterator<DataSample> iterator() {
		return new DataSampleIterator();
	}

	private class DataSampleIterator implements Iterator<DataSample> {

		private int cursor = 0;

		private int size = positions.getSize();

		private DataSample sample = new DataSample(discreteFeatures, continuousFeatures);

		@Override
		public boolean hasNext() {
			return cursor < size;
		}

		@Override
		public DataSample next() {
			sample.update(positions.getData(cursor), getMark(cursor));
			cursor++;
			return sample;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

}
