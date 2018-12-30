package com.jstarcraft.recommendation.recommender.collaborative.ranking;

import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.gradient.DefaultGradient;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.layers.BaseLayer;
import org.deeplearning4j.nn.params.DefaultParamInitializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;

/**
 * 
 * DeepFM输入层
 * 
 * <pre>
 * DeepFM: A Factorization-Machine based Neural Network for CTR Prediction
 * </pre>
 * 
 * @author Birdy
 *
 */
public class DeepFMInputLayer extends BaseLayer<DeepFMInputConfiguration> {

	private int[] dimensionSizes;

	public DeepFMInputLayer(NeuralNetConfiguration configuration, int[] dimensionSizes) {
		super(configuration);
		this.dimensionSizes = dimensionSizes;
	}

	@Override
	public Pair<Gradient, INDArray> backpropGradient(INDArray previous) {
		INDArray output = this.preOutput(true);
		INDArray delta = layerConf().getActivationFn().backprop(output, previous).getFirst();

		Gradient gradient = new DefaultGradient();
		INDArray weight = gradientViews.get(DefaultParamInitializer.WEIGHT_KEY);
		INDArray bias = gradientViews.get(DefaultParamInitializer.BIAS_KEY);

		weight.assign(0D);
		for (int index = 0; index < input.rows(); index++) {
			for (int column = 0; column < delta.columns(); column++) {
				int cursor = 0;
				for (int dimension = 0; dimension < dimensionSizes.length; dimension++) {
					int point = cursor + input.getInt(index, dimension);
					double value = weight.getDouble(point, column);
					value += delta.getDouble(index, column);
					weight.put(point, column, value);
					cursor += dimensionSizes[dimension];
				}
			}
		}
		delta.sum(bias, 0);

		gradient.gradientForVariable().put(DefaultParamInitializer.WEIGHT_KEY, weight);
		gradient.gradientForVariable().put(DefaultParamInitializer.BIAS_KEY, bias);

		// INDArray next =
		// params.get(DefaultParamInitializer.WEIGHT_KEY).mmul(delta.transpose()).transpose();
		return new Pair<>(gradient, null);
	}

	@Override
	public boolean isPretrainLayer() {
		return false;
	}

	@Override
	public INDArray preOutput(boolean training) {
		INDArray weight = getParam(DefaultParamInitializer.WEIGHT_KEY);
		INDArray bias = getParam(DefaultParamInitializer.BIAS_KEY);

		INDArray output = Nd4j.zeros(input.rows(), weight.columns());
		for (int row = 0; row < input.rows(); row++) {
			for (int column = 0; column < weight.columns(); column++) {
				double value = 0D;
				int cursor = 0;
				for (int index = 0; index < input.columns(); index++) {
					value += weight.getDouble(cursor + input.getInt(row, index), column);
					cursor += dimensionSizes[index];
				}
				output.put(row, column, value);
			}
		}
		output.addiRowVector(bias);
		return output;
	}

}
