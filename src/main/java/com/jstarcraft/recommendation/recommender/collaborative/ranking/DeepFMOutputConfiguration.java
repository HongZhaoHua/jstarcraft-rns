package com.jstarcraft.recommendation.recommender.collaborative.ranking;

import java.util.Collection;
import java.util.Map;

import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.ParamInitializer;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.BaseOutputLayer;
import org.deeplearning4j.nn.params.DefaultParamInitializer;
import org.deeplearning4j.optimize.api.IterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.lossfunctions.ILossFunction;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

/**
 * 
 * DeepFM输出配置
 * 
 * <pre>
 * DeepFM: A Factorization-Machine based Neural Network for CTR Prediction
 * </pre>
 * 
 * @author Birdy
 *
 */
public class DeepFMOutputConfiguration extends BaseOutputLayer {

	DeepFMOutputConfiguration() {
	}

	protected DeepFMOutputConfiguration(Builder builder) {
		super(builder);
	}

	@Override
	public ParamInitializer initializer() {
		return DefaultParamInitializer.getInstance();
	}

	@Override
	public Layer instantiate(NeuralNetConfiguration configuration, Collection<IterationListener> monitors, int layerIndex, INDArray parameters, boolean initialize) {
		DeepFMOutputLayer layer = new DeepFMOutputLayer(configuration);
		layer.setListeners(monitors);
		layer.setIndex(layerIndex);
		layer.setParamsViewArray(parameters);
		Map<String, INDArray> table = initializer().init(configuration, parameters, initialize);
		layer.setParamTable(table);
		layer.setConf(configuration);
		return layer;
	}

	public static class Builder extends BaseOutputLayer.Builder<Builder> {

		public Builder(LossFunction lossFunction) {
			super.lossFunction(lossFunction);
		}

		public Builder(ILossFunction lossFunction) {
			this.lossFn = lossFunction;
		}

		@Override
		public DeepFMOutputConfiguration build() {
			return new DeepFMOutputConfiguration(this);
		}

	}

}
