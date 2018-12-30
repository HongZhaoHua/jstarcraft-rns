package com.jstarcraft.recommendation.recommender.collaborative.ranking;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.distribution.Distributions;
import org.deeplearning4j.nn.conf.layers.FeedForwardLayer;
import org.deeplearning4j.nn.params.DefaultParamInitializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.rng.distribution.Distribution;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;

/**
 * 
 * DeepFM参数
 * 
 * <pre>
 * DeepFM: A Factorization-Machine based Neural Network for CTR Prediction
 * </pre>
 * 
 * @author Birdy
 *
 */
class DeepFMParameter extends DefaultParamInitializer {

	private int numberOfFeatures;

	public int[] dimensionSizes;

	public DeepFMParameter(int... dimensionSizes) {
		this.dimensionSizes = dimensionSizes;
		this.numberOfFeatures = 0;
		for (int dimensionSize : dimensionSizes) {
			numberOfFeatures += dimensionSize;
		}
	}

	@Override
	public int numParams(NeuralNetConfiguration configuration) {
		FeedForwardLayer layerConfiguration = (FeedForwardLayer) configuration.getLayer();
		return numberOfFeatures * layerConfiguration.getNOut() + layerConfiguration.getNOut();
	}

	protected INDArray createWeightMatrix(NeuralNetConfiguration configuration, INDArray view, boolean initialize) {
		FeedForwardLayer layerConfiguration = (FeedForwardLayer) configuration.getLayer();
		if (initialize) {
			Distribution distribution = Distributions.createDistribution(layerConfiguration.getDist());
			return super.createWeightMatrix(numberOfFeatures, layerConfiguration.getNOut(), layerConfiguration.getWeightInit(), distribution, view, true);
		} else {
			return super.createWeightMatrix(numberOfFeatures, layerConfiguration.getNOut(), null, null, view, false);
		}
	}

	@Override
	public Map<String, INDArray> init(NeuralNetConfiguration configuration, INDArray view, boolean initialize) {
		Map<String, INDArray> parameters = Collections.synchronizedMap(new LinkedHashMap<String, INDArray>());
		FeedForwardLayer layerConfiguration = (FeedForwardLayer) configuration.getLayer();
		int numberOfOut = layerConfiguration.getNOut();
		int numberOfWeights = numberOfFeatures * numberOfOut;
		INDArray weight = view.get(new INDArrayIndex[] { NDArrayIndex.point(0), NDArrayIndex.interval(0, numberOfWeights) });
		INDArray bias = view.get(NDArrayIndex.point(0), NDArrayIndex.interval(numberOfWeights, numberOfWeights + numberOfOut));

		parameters.put(WEIGHT_KEY, this.createWeightMatrix(configuration, weight, initialize));
		parameters.put(BIAS_KEY, createBias(configuration, bias, initialize));
		configuration.addVariable(WEIGHT_KEY);
		configuration.addVariable(BIAS_KEY);
		return parameters;
	}

	@Override
	public Map<String, INDArray> getGradientsFromFlattened(NeuralNetConfiguration configuration, INDArray view) {
		Map<String, INDArray> gradients = new LinkedHashMap<>();
		FeedForwardLayer layerConfiguration = (FeedForwardLayer) configuration.getLayer();
		int numberOfOut = layerConfiguration.getNOut();
		int numberOfWeights = numberOfFeatures * numberOfOut;
		INDArray weight = view.get(NDArrayIndex.point(0), NDArrayIndex.interval(0, numberOfWeights)).reshape('f', numberOfWeights, numberOfOut);
		INDArray bias = view.get(NDArrayIndex.point(0), NDArrayIndex.interval(numberOfWeights, numberOfWeights + numberOfOut));
		gradients.put(WEIGHT_KEY, weight);
		gradients.put(BIAS_KEY, bias);
		return gradients;
	}
}
