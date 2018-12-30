package com.jstarcraft.recommendation.recommender.collaborative.ranking;

import java.util.Collection;
import java.util.Map;

import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.ParamInitializer;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.FeedForwardLayer;
import org.deeplearning4j.nn.conf.memory.LayerMemoryReport;
import org.deeplearning4j.nn.conf.memory.MemoryReport;
import org.deeplearning4j.optimize.api.IterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * 
 * CDAE配置
 * 
 * <pre>
 * Collaborative Denoising Auto-Encoders for Top-N Recommender Systems
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class CDAEConfiguration extends FeedForwardLayer {

	private CDAEParameter cdaeParameter;

	CDAEConfiguration() {
		// We need a no-arg constructor so we can deserialize the configuration
		// from JSON or YAML format
		// Without this, you will likely get an exception like the following:
		// com.fasterxml.jackson.databind.JsonMappingException: No suitable
		// constructor found for type [simple type, class
		// org.deeplearning4j.examples.misc.customlayers.layer.CustomLayer]: can
		// not instantiate from JSON object (missing default constructor or
		// creator, or perhaps need to add/enable type information?)
	}

	private CDAEConfiguration(Builder builder) {
		super(builder);
		this.cdaeParameter = new CDAEParameter(builder.numberOfUsers);
	}

	@Override
	public Layer instantiate(NeuralNetConfiguration conf, Collection<IterationListener> iterationListeners, int layerIndex, INDArray layerParamsView, boolean initializeParams) {
		// The instantiate method is how we go from the configuration class
		// (i.e., this class) to the implementation class
		// (i.e., a CustomLayerImpl instance)
		// For the most part, it's the same for each type of layer

		CDAELayer myCustomLayer = new CDAELayer(conf);
		myCustomLayer.setListeners(iterationListeners); // Set the iteration
														// listeners, if any
		myCustomLayer.setIndex(layerIndex); // Integer index of the layer

		// Parameter view array: In Deeplearning4j, the network parameters for
		// the entire network (all layers) are
		// allocated in one big array. The relevant section of this parameter
		// vector is extracted out for each layer,
		// (i.e., it's a "view" array in that it's a subset of a larger array)
		// This is a row vector, with length equal to the number of parameters
		// in the layer
		myCustomLayer.setParamsViewArray(layerParamsView);

		// Initialize the layer parameters. For example,
		// Note that the entries in paramTable (2 entries here: a weight array
		// of shape [nIn,nOut] and biases of shape [1,nOut]
		// are in turn a view of the 'layerParamsView' array.
		Map<String, INDArray> paramTable = initializer().init(conf, layerParamsView, initializeParams);
		myCustomLayer.setParamTable(paramTable);
		myCustomLayer.setConf(conf);
		return myCustomLayer;
	}

	@Override
	public ParamInitializer initializer() {
		// This method returns the parameter initializer for this type of layer
		// In this case, we can use the DefaultParamInitializer, which is the
		// same one used for DenseLayer
		// For more complex layers, you may need to implement a custom parameter
		// initializer
		// See the various parameter initializers here:
		// https://github.com/deeplearning4j/deeplearning4j/tree/master/deeplearning4j-core/src/main/java/org/deeplearning4j/nn/params
		return cdaeParameter;
	}

	@Override
	public double getL1ByParam(String paramName) {
		switch (paramName) {
		case CDAEParameter.WEIGHT_KEY:
			return l1;
		case CDAEParameter.BIAS_KEY:
			return l1Bias;
		case CDAEParameter.USER_KEY:
			return l1;
		default:
			throw new IllegalArgumentException("Unknown parameter name: \"" + paramName + "\"");
		}
	}

	@Override
	public double getL2ByParam(String paramName) {
		switch (paramName) {
		case CDAEParameter.WEIGHT_KEY:
			return l2;
		case CDAEParameter.BIAS_KEY:
			return l2Bias;
		case CDAEParameter.USER_KEY:
			return l2;
		default:
			throw new IllegalArgumentException("Unknown parameter name: \"" + paramName + "\"");
		}
	}

	// Here's an implementation of a builder pattern, to allow us to easily
	// configure the layer
	// Note that we are inheriting all of the FeedForwardLayer.Builder options:
	// things like n
	public static class Builder extends FeedForwardLayer.Builder<Builder> {
		private int numberOfUsers;

		@Override
		@SuppressWarnings("unchecked") // To stop warnings about unchecked cast.
										// Not required.
		public CDAEConfiguration build() {
			return new CDAEConfiguration(this);
		}

		public Builder setNumUsers(int numUsers) {
			this.numberOfUsers = numUsers;
			return this;
		}
	}

	@Override
	public LayerMemoryReport getMemoryReport(InputType inputType) {
		return new LayerMemoryReport.Builder(layerName, CDAEConfiguration.class, inputType, inputType).standardMemory(0, 0) // No params
				.workingMemory(0, 0, 0, 0).cacheMemory(MemoryReport.CACHE_MODE_ALL_ZEROS, MemoryReport.CACHE_MODE_ALL_ZEROS) // No caching
				.build();
	}
}
