package com.jstarcraft.recommendation.data.accessor;

import java.util.LinkedHashMap;

import com.jstarcraft.ai.data.attribute.QualityAttribute;
import com.jstarcraft.ai.data.attribute.QuantityAttribute;
import com.jstarcraft.ai.utility.FloatArray;
import com.jstarcraft.ai.utility.IntegerArray;

/**
 * 常量标记器
 * 
 * @author Birdy
 *
 */
public class ConstantMarker extends SampleAccessor {

	private float constant;

	public ConstantMarker(IntegerArray positions, DenseModule model, float constant) {
		this.discreteAttributes = new QualityAttribute[model.qualityAttributes.length];
		this.continuousAttributes = new QuantityAttribute[model.quantityAttributes.length];
		this.discreteFeatures = new IntegerArray[model.qualityAttributes.length];
		this.continuousFeatures = new FloatArray[model.quantityAttributes.length];
		for (int index = 0, size = model.qualityAttributes.length; index < size; index++) {
			this.discreteAttributes[index] = model.qualityAttributes[index];
			this.discreteFeatures[index] = model.qualityFeatures[index];
		}
		for (int index = 0, size = model.quantityAttributes.length; index < size; index++) {
			this.continuousAttributes[index++] = model.quantityAttributes[index];
			this.continuousFeatures[index++] = model.quantityFeatures[index];
		}
		this.discreteDimensions = new LinkedHashMap<>();
		for (String field : model.getQualityFields()) {
			this.discreteDimensions.put(field, model.getQualityDimension(field));
		}
		this.continuousDimensions = new LinkedHashMap<>();
		for (String field : model.getQuantityFields()) {
			this.continuousDimensions.put(field, model.getQuantityDimension(field));
		}
		this.positions = positions;
		this.constant = constant;
	}

	@Override
	public float getMark(int position) {
		return constant;
	}

}
