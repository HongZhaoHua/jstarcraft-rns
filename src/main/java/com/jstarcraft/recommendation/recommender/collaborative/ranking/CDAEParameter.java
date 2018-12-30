package com.jstarcraft.recommendation.recommender.collaborative.ranking;

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
 * CDAE参数
 * 
 * <pre>
 * Collaborative Denoising Auto-Encoders for Top-N Recommender Systems
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
class CDAEParameter extends DefaultParamInitializer {

	public static final String USER_KEY = "u";

	public int numberOfUsers;

	public CDAEParameter(int numberOfUsers) {
		this.numberOfUsers = numberOfUsers;
	}

	@Override
	public int numParams(NeuralNetConfiguration conf) {
		FeedForwardLayer layerConf = (FeedForwardLayer) conf.getLayer();
		return super.numParams(conf) + numberOfUsers * layerConf.getNOut(); // add
		// another
		// user
		// weight
		// matrix
	}

	private INDArray createUserWeightMatrix(NeuralNetConfiguration conf, INDArray weightParamView, boolean initializeParameters) {
		FeedForwardLayer layerConf = (FeedForwardLayer) conf.getLayer();
		if (initializeParameters) {
			Distribution dist = Distributions.createDistribution(layerConf.getDist());
			return createWeightMatrix(numberOfUsers, layerConf.getNOut(), layerConf.getWeightInit(), dist, weightParamView, true);
		} else {
			return createWeightMatrix(numberOfUsers, layerConf.getNOut(), null, null, weightParamView, false);
		}
	}

	@Override
	public Map<String, INDArray> init(NeuralNetConfiguration conf, INDArray paramsView, boolean initializeParams) {
		Map<String, INDArray> params = super.init(conf, paramsView, initializeParams);
		FeedForwardLayer layerConf = (FeedForwardLayer) conf.getLayer();
		int nIn = layerConf.getNIn();
		int nOut = layerConf.getNOut();
		int nWeightParams = nIn * nOut;
		int nUserWeightParams = numberOfUsers * nOut;
		INDArray userWeightView = paramsView.get(new INDArrayIndex[] { NDArrayIndex.point(0), NDArrayIndex.interval(nWeightParams + nOut, nWeightParams + nOut + nUserWeightParams) });
		params.put(USER_KEY, this.createUserWeightMatrix(conf, userWeightView, initializeParams));
		conf.addVariable(USER_KEY);
		return params;
	}

	@Override
	public Map<String, INDArray> getGradientsFromFlattened(NeuralNetConfiguration conf, INDArray gradientView) {
		Map<String, INDArray> out = super.getGradientsFromFlattened(conf, gradientView);
		FeedForwardLayer layerConf = (FeedForwardLayer) conf.getLayer();
		int nIn = layerConf.getNIn();
		int nOut = layerConf.getNOut();
		int nWeightParams = nIn * nOut;
		int nUserWeightParams = numberOfUsers * nOut;
		INDArray userWeightGradientView = gradientView.get(NDArrayIndex.point(0), NDArrayIndex.interval(nWeightParams + nOut, nWeightParams + nOut + nUserWeightParams)).reshape('f', numberOfUsers, nOut);
		out.put(USER_KEY, userWeightGradientView);
		return out;
	}
}
