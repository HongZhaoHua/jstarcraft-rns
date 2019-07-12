package com.jstarcraft.rns.recommend.neuralnetwork;

import java.util.Map;

import com.jstarcraft.ai.math.structure.MathCache;
import com.jstarcraft.ai.math.structure.matrix.MathMatrix;
import com.jstarcraft.ai.model.neuralnetwork.activation.ActivationFunction;
import com.jstarcraft.ai.model.neuralnetwork.layer.ParameterConfigurator;
import com.jstarcraft.ai.model.neuralnetwork.layer.WeightLayer;
import com.jstarcraft.core.utility.KeyValue;

/**
 * 
 * FM层
 * 
 * <pre>
 * DeepFM: A Factorization-Machine based Neural Network for CTR Prediction
 * </pre>
 * 
 * @author Birdy
 *
 */
public class FMLayer extends WeightLayer {

	private int[] dimensionSizes;

	protected FMLayer() {
		super();
	}

	public FMLayer(int[] dimensionSizes, int numberOfInputs, int numberOfOutputs, MathCache factory, Map<String, ParameterConfigurator> configurators, ActivationFunction function) {
		super(numberOfInputs, numberOfOutputs, factory, configurators, function);
		this.dimensionSizes = dimensionSizes;
	}

	@Override
	public void doCache(MathCache factory, KeyValue<MathMatrix, MathMatrix> samples) {
		inputKeyValue = samples;
		int rowSize = inputKeyValue.getKey().getRowSize();
		int columnSize = inputKeyValue.getKey().getColumnSize();

		// 检查维度
		if (this.dimensionSizes.length != columnSize) {
			throw new IllegalArgumentException();
		}

		middleKeyValue = new KeyValue<>(null, null);
		outputKeyValue = new KeyValue<>(null, null);

		MathMatrix middleData = factory.makeMatrix(rowSize, numberOfOutputs);
		middleKeyValue.setKey(middleData);
		MathMatrix middleError = factory.makeMatrix(rowSize, numberOfOutputs);
		middleKeyValue.setValue(middleError);

		MathMatrix outputData = factory.makeMatrix(rowSize, numberOfOutputs);
		outputKeyValue.setKey(outputData);
		MathMatrix innerError = factory.makeMatrix(rowSize, numberOfOutputs);
		outputKeyValue.setValue(innerError);
	}

	@Override
	public void doForward() {
		MathMatrix weightParameters = parameters.get(WEIGHT_KEY);
		MathMatrix biasParameters = parameters.get(BIAS_KEY);

		MathMatrix inputData = inputKeyValue.getKey();
		MathMatrix middleData = middleKeyValue.getKey();
		MathMatrix outputData = outputKeyValue.getKey();

		// inputData.dotProduct(weightParameters, middleData);
		for (int row = 0; row < inputData.getRowSize(); row++) {
			for (int column = 0; column < weightParameters.getColumnSize(); column++) {
				float value = 0F;
				int cursor = 0;
				for (int index = 0; index < inputData.getColumnSize(); index++) {
					value += weightParameters.getValue(cursor + (int) inputData.getValue(row, index), column);
					cursor += dimensionSizes[index];
				}
				middleData.setValue(row, column, value);
			}
		}
		if (biasParameters != null) {
			for (int columnIndex = 0, columnSize = middleData.getColumnSize(); columnIndex < columnSize; columnIndex++) {
				float bias = biasParameters.getValue(0, columnIndex);
				middleData.getColumnVector(columnIndex).shiftValues(bias);
			}
		}

		function.forward(middleData, outputData);

		MathMatrix middleError = middleKeyValue.getValue();
		middleError.setValues(0F);

		MathMatrix innerError = outputKeyValue.getValue();
		innerError.setValues(0F);
	}

	@Override
	public void doBackward() {
		MathMatrix weightParameters = parameters.get(WEIGHT_KEY);
		MathMatrix biasParameters = parameters.get(BIAS_KEY);
		MathMatrix weightGradients = gradients.get(WEIGHT_KEY);
		MathMatrix biasGradients = gradients.get(BIAS_KEY);

		MathMatrix innerError = outputKeyValue.getValue();
		MathMatrix middleError = middleKeyValue.getValue();
		// 必须为null
		MathMatrix outerError = inputKeyValue.getValue();

		MathMatrix inputData = inputKeyValue.getKey();
		MathMatrix middleData = middleKeyValue.getKey();
		MathMatrix outputData = outputKeyValue.getKey();

		// 计算梯度
		function.backward(middleData, innerError, middleError);

		// inputData.transposeProductThat(middleError, weightGradients);
		weightGradients.setValues(0F);
		for (int index = 0; index < inputData.getRowSize(); index++) {
			for (int column = 0; column < middleError.getColumnSize(); column++) {
				int cursor = 0;
				for (int dimension = 0; dimension < dimensionSizes.length; dimension++) {
					int point = cursor + (int) inputData.getValue(index, dimension);
					weightGradients.shiftValue(point, column, middleError.getValue(index, column));
					cursor += dimensionSizes[dimension];
				}
			}
		}
		if (biasGradients != null) {
			for (int columnIndex = 0, columnSize = biasGradients.getColumnSize(); columnIndex < columnSize; columnIndex++) {
				float bias = middleError.getColumnVector(columnIndex).getSum(false);
				biasGradients.setValue(0, columnIndex, bias);
			}
		}
	}

}
