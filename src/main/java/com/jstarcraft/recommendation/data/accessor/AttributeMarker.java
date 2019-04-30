package com.jstarcraft.recommendation.data.accessor;

import java.util.LinkedHashMap;

import com.jstarcraft.ai.data.attribute.QualityAttribute;
import com.jstarcraft.ai.data.attribute.QuantityAttribute;
import com.jstarcraft.ai.utility.FloatArray;
import com.jstarcraft.ai.utility.IntegerArray;

/**
 * 属性标记器
 * 
 * @author Birdy
 *
 */
public class AttributeMarker extends SampleAccessor {

	private FloatArray scores;

	public AttributeMarker(IntegerArray positions, DenseModule model, String scoreField) {
		this.discreteAttributes = new QualityAttribute[model.qualityAttributes.length];
		this.continuousAttributes = new QuantityAttribute[model.quantityAttributes.length - 1];
		this.discreteFeatures = new IntegerArray[model.qualityAttributes.length];
		this.continuousFeatures = new FloatArray[model.quantityAttributes.length - 1];
		for (int index = 0, size = model.qualityAttributes.length; index < size; index++) {
			this.discreteAttributes[index] = model.qualityAttributes[index];
			this.discreteFeatures[index] = model.qualityFeatures[index];
		}
		int scoreDimension = model.getQuantityDimension(scoreField);
		int featureDimension = 0;
		for (int index = 0, size = model.quantityAttributes.length; index < size; index++) {
			if (scoreDimension != index) {
				this.continuousAttributes[featureDimension] = model.quantityAttributes[index];
				this.continuousFeatures[featureDimension] = model.quantityFeatures[index];
				featureDimension++;
			}
		}
		this.discreteDimensions = new LinkedHashMap<>();
		featureDimension = 0;
		for (String field : model.getQualityFields()) {
			this.discreteDimensions.put(field, featureDimension++);
		}
		this.continuousDimensions = new LinkedHashMap<>();
		featureDimension = 0;
		for (String field : model.getQuantityFields()) {
			if (field.equals(scoreField)) {
				continue;
			}
			this.continuousDimensions.put(field, featureDimension++);
		}
		this.positions = positions;
		this.scores = model.quantityFeatures[scoreDimension];
	}

	@Override
	public float getMark(int position) {
		return scores.getData(positions.getData(position));
	}

}
