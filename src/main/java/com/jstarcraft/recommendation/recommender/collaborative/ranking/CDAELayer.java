package com.jstarcraft.recommendation.recommender.collaborative.ranking;

import java.util.Arrays;

import org.deeplearning4j.exception.DL4JInvalidInputException;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.gradient.DefaultGradient;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.layers.BaseLayer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;

/**
 * 
 * CDAE层
 * 
 * <pre>
 * Collaborative Denoising Auto-Encoders for Top-N Recommender Systems
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class CDAELayer extends BaseLayer<CDAEConfiguration> {

	public CDAELayer(NeuralNetConfiguration conf) {
		super(conf);
	}

	@Override
	public INDArray preOutput(INDArray x, boolean training) {
		if (x == null) {
			throw new IllegalArgumentException("No null input allowed");
		} else {
			this.setInput(x);
			return this.preOutput(training);
		}
	}

	@Override
	public INDArray preOutput(boolean training) {
		/*
		 * The preOut method(s) calculate the activations (forward pass), before the
		 * activation function is applied.
		 * 
		 * Because we aren't doing anything different to a standard dense layer, we can
		 * use the existing implementation for this. Other network types (RNNs, CNNs
		 * etc) will require you to implement this method.
		 * 
		 * For custom layers, you may also have to implement methods such as calcL1,
		 * calcL2, numParams, etc.
		 */
		applyDropOutIfNecessary(training);
		INDArray b = getParam(CDAEParameter.BIAS_KEY);
		INDArray W = getParam(CDAEParameter.WEIGHT_KEY);
		INDArray U = getParam(CDAEParameter.USER_KEY);

		// Input validation:
		if (input.rank() != 2 || input.columns() != W.rows()) {
			if (input.rank() != 2) {
				throw new DL4JInvalidInputException("Input that is not a matrix; expected matrix (rank 2), got rank " + input.rank() + " array with shape " + Arrays.toString(input.shape()) + ". Missing preprocessor or wrong input type? " + this.layerId());
			}
			throw new DL4JInvalidInputException("Input size (" + input.columns() + " columns; shape = " + Arrays.toString(input.shape()) + ") is invalid: does not match layer input size (layer # inputs = " + W.size(0) + ") " + this.layerId());
		}

		// modified preOut: WX + V + b
		INDArray ret = input.mmul(W).addi(U).addiRowVector(b);

		if (maskArray != null) {
			applyMask(ret);
		}

		return ret;
	}

	@Override
	public INDArray activate(boolean training) {
		return super.activate(training);
	}

	@Override
	public boolean isPretrainLayer() {
		return false;
	}

	@Override
	public Pair<Gradient, INDArray> backpropGradient(INDArray epsilon) {
		// If this layer is layer L, then epsilon is (w^(L+1)*(d^(L+1))^T) (or
		// equivalent)
		INDArray z = this.preOutput(true); // Note: using preOutput(INDArray)
											// can't be used as this does a
											// setInput(input) and resets the
											// 'appliedDropout' flag
		// INDArray activationDerivative =
		// Nd4j.getExecutioner().execAndReturn(Nd4j.getOpFactory().createTransform(conf().getLayer().getActivationFunction(),
		// z).derivative());
		// INDArray activationDerivative =
		// conf().getLayer().getActivationFn().getGradient(z);
		// INDArray delta = epsilon.muli(activationDerivative);
		INDArray delta = layerConf().getActivationFn().backprop(z, epsilon).getFirst(); // TODO
																						// handle
																						// activation
																						// function
																						// params

		if (maskArray != null) {
			applyMask(delta);
		}

		Gradient ret = new DefaultGradient();

		INDArray weightGrad = gradientViews.get(CDAEParameter.WEIGHT_KEY); // f
																			// order
		Nd4j.gemm(input, delta, weightGrad, true, false, 1.0, 0.0);
		INDArray userWeightGrad = gradientViews.get(CDAEParameter.USER_KEY); // f
																				// order
		userWeightGrad.assign(delta);
		INDArray biasGrad = gradientViews.get(CDAEParameter.BIAS_KEY);
		biasGrad.assign(delta.sum(0)); // biasGrad is initialized/zeroed first

		ret.gradientForVariable().put(CDAEParameter.WEIGHT_KEY, weightGrad);
		ret.gradientForVariable().put(CDAEParameter.BIAS_KEY, biasGrad);
		ret.gradientForVariable().put(CDAEParameter.USER_KEY, userWeightGrad);

		INDArray epsilonNext = params.get(CDAEParameter.WEIGHT_KEY).mmul(delta.transpose()).transpose();
		// epsilonNext = null;
		return new Pair<>(ret, epsilonNext);
	}

	@Override
	public double calcL2(boolean backpropParamsOnly) {
		double l2Sum = 0.0D;
		double l2Norm;
		if (this.conf.getL2ByParam(CDAEParameter.WEIGHT_KEY) > 0.0D) {
			l2Norm = this.getParam(CDAEParameter.WEIGHT_KEY).norm2Number().doubleValue();
			l2Sum += 0.5D * this.conf.getL2ByParam(CDAEParameter.WEIGHT_KEY) * l2Norm * l2Norm;
		}
		if (this.conf.getL2ByParam(CDAEParameter.USER_KEY) > 0.0D) {
			l2Norm = this.getParam(CDAEParameter.USER_KEY).norm2Number().doubleValue();
			l2Sum += 0.5D * this.conf.getL2ByParam(CDAEParameter.USER_KEY) * l2Norm * l2Norm;
		}
		if (this.conf.getL2ByParam(CDAEParameter.BIAS_KEY) > 0.0D) {
			l2Norm = this.getParam(CDAEParameter.BIAS_KEY).norm2Number().doubleValue();
			l2Sum += 0.5D * this.conf.getL2ByParam(CDAEParameter.BIAS_KEY) * l2Norm * l2Norm;
		}
		return l2Sum;
	}

}
