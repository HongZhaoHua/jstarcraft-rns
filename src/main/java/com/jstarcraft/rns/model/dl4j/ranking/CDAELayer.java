package com.jstarcraft.rns.model.dl4j.ranking;

import java.util.Arrays;

import org.deeplearning4j.exception.DL4JInvalidInputException;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.gradient.DefaultGradient;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.layers.BaseLayer;
import org.deeplearning4j.nn.params.DefaultParamInitializer;
import org.deeplearning4j.nn.workspace.ArrayType;
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr;
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
//TODO 存档,以后需要基于DL4J重构.
@Deprecated
public class CDAELayer extends BaseLayer<CDAEConfiguration> {

    public CDAELayer(NeuralNetConfiguration conf) {
        super(conf);
    }

    @Override
    public INDArray preOutput(boolean training, LayerWorkspaceMgr workspaceMgr) {
        assertInputSet(false);
        applyDropOutIfNecessary(training, workspaceMgr);
        INDArray W = getParamWithNoise(DefaultParamInitializer.WEIGHT_KEY, training, workspaceMgr);
        INDArray U = getParamWithNoise(CDAEParameter.USER_KEY, training, workspaceMgr);
        INDArray b = getParamWithNoise(DefaultParamInitializer.BIAS_KEY, training, workspaceMgr);

        // Input validation:
        if (input.rank() != 2 || input.columns() != W.rows()) {
            if (input.rank() != 2) {
                throw new DL4JInvalidInputException("Input that is not a matrix; expected matrix (rank 2), got rank " + input.rank() + " array with shape " + Arrays.toString(input.shape()) + ". Missing preprocessor or wrong input type? " + layerId());
            }
            throw new DL4JInvalidInputException("Input size (" + input.columns() + " columns; shape = " + Arrays.toString(input.shape()) + ") is invalid: does not match layer input size (layer # inputs = " + W.size(0) + ") " + layerId());
        }

        INDArray ret = workspaceMgr.createUninitialized(ArrayType.ACTIVATIONS, input.size(0), W.size(1));
        input.mmuli(W, ret);
        ret.addi(U);
        if (hasBias()) {
            ret.addiRowVector(b);
        }

        if (maskArray != null) {
            applyMask(ret);
        }

        return ret;
    }

    @Override
    public Pair<Gradient, INDArray> backpropGradient(INDArray epsilon, LayerWorkspaceMgr workspaceMgr) {
        assertInputSet(true);
        // If this layer is layer L, then epsilon is (w^(L+1)*(d^(L+1))^T) (or
        // equivalent)
        INDArray z = preOutput(true, workspaceMgr); // Note: using preOutput(INDArray) can't be used as this does a setInput(input)
                                                    // and resets the 'appliedDropout' flag
        // INDArray activationDerivative =
        // Nd4j.getExecutioner().execAndReturn(Nd4j.getOpFactory().createTransform(conf().getLayer().getActivationFunction(),
        // z).derivative());
        // INDArray activationDerivative =
        // conf().getLayer().getActivationFn().getGradient(z);
        // INDArray delta = epsilon.muli(activationDerivative);
        INDArray delta = layerConf().getActivationFn().backprop(z, epsilon).getFirst(); // TODO handle activation function params

        if (maskArray != null) {
            applyMask(delta);
        }

        Gradient ret = new DefaultGradient();

        INDArray weightGrad = gradientViews.get(DefaultParamInitializer.WEIGHT_KEY); // f order
        Nd4j.gemm(input, delta, weightGrad, true, false, 1.0, 0.0);
        ret.gradientForVariable().put(DefaultParamInitializer.WEIGHT_KEY, weightGrad);

        INDArray userWeightGrad = gradientViews.get(CDAEParameter.USER_KEY);
        userWeightGrad.assign(delta);
        ret.gradientForVariable().put(CDAEParameter.USER_KEY, userWeightGrad);

        if (hasBias()) {
            INDArray biasGrad = gradientViews.get(DefaultParamInitializer.BIAS_KEY);
            delta.sum(biasGrad, 0); // biasGrad is initialized/zeroed first
            ret.gradientForVariable().put(DefaultParamInitializer.BIAS_KEY, biasGrad);
        }

        INDArray W = getParamWithNoise(DefaultParamInitializer.WEIGHT_KEY, true, workspaceMgr);
        INDArray epsilonNext = workspaceMgr.createUninitialized(ArrayType.ACTIVATION_GRAD, new long[] { W.size(0), delta.size(0) }, 'f');
        epsilonNext = W.mmuli(delta.transpose(), epsilonNext).transpose(); // W.mmul(delta.transpose()).transpose();

        weightNoiseParams.clear();

        epsilonNext = backpropDropOutIfPresent(epsilonNext);
        return new Pair<>(ret, epsilonNext);
    }

    @Override
    public boolean isPretrainLayer() {
        return false;
    }

}
