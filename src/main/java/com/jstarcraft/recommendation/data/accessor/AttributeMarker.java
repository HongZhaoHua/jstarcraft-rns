package com.jstarcraft.recommendation.data.accessor;

import java.util.LinkedHashMap;

import com.jstarcraft.ai.utility.IntegerArray;
import com.jstarcraft.recommendation.data.QuantityAttribute;
import com.jstarcraft.recommendation.data.QualityAttribute;

/**
 * 属性标记器
 * 
 * @author Birdy
 *
 */
public class AttributeMarker extends SampleAccessor {

	private float[] scores;

	public AttributeMarker(IntegerArray positions, InstanceAccessor model, String scoreField) {
		this.discreteAttributes = new QualityAttribute[model.discreteAttributes.length];
		this.continuousAttributes = new QuantityAttribute[model.continuousAttributes.length - 1];
		this.discreteFeatures = new int[model.discreteAttributes.length][];
		this.continuousFeatures = new float[model.continuousAttributes.length - 1][];
		for (int index = 0, size = model.discreteAttributes.length; index < size; index++) {
			this.discreteAttributes[index] = model.discreteAttributes[index];
			this.discreteFeatures[index] = model.discreteFeatures[index];
		}
		int scoreDimension = model.getContinuousDimension(scoreField);
		int featureDimension = 0;
		for (int index = 0, size = model.continuousAttributes.length; index < size; index++) {
			if (scoreDimension != index) {
				this.continuousAttributes[featureDimension] = model.continuousAttributes[index];
				this.continuousFeatures[featureDimension] = model.continuousFeatures[index];
				featureDimension++;
			}
		}
		this.discreteDimensions = new LinkedHashMap<>();
		featureDimension = 0;
		for (String field : model.getDiscreteFields()) {
			this.discreteDimensions.put(field, featureDimension++);
		}
		this.continuousDimensions = new LinkedHashMap<>();
		featureDimension = 0;
		for (String field : model.getContinuousFields()) {
			if (field.equals(scoreField)) {
				continue;
			}
			this.continuousDimensions.put(field, featureDimension++);
		}
		this.positions = positions;
		this.scores = model.continuousFeatures[scoreDimension];
	}

	@Override
	public float getMark(int position) {
		return scores[positions.getData(position)];
	}

}
