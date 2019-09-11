package com.jstarcraft.rns.model.dl4j.ranking;

import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.gradient.DefaultGradient;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.layers.BaseLayer;
import org.deeplearning4j.nn.params.DefaultParamInitializer;
import org.deeplearning4j.nn.workspace.ArrayType;
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr;
import org.nd4j.linalg.api.ndarray.INDArray;
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
//TODO 存档,以后需要基于DL4J重构.
@Deprecated
public class DeepFMInputLayer extends BaseLayer<DeepFMInputConfiguration> {

    private int[] dimensionSizes;

    public DeepFMInputLayer(NeuralNetConfiguration configuration, int[] dimensionSizes) {
        super(configuration);
        this.dimensionSizes = dimensionSizes;
    }

    @Override
    public INDArray preOutput(boolean training, LayerWorkspaceMgr workspaceMgr) {
        assertInputSet(false);
        applyDropOutIfNecessary(training, workspaceMgr);
        INDArray W = getParamWithNoise(DefaultParamInitializer.WEIGHT_KEY, training, workspaceMgr);
        INDArray b = getParamWithNoise(DefaultParamInitializer.BIAS_KEY, training, workspaceMgr);

        INDArray ret = workspaceMgr.createUninitialized(ArrayType.ACTIVATIONS, input.size(0), W.size(1));
        ret.assign(0F);
        for (int row = 0; row < input.rows(); row++) {
            for (int column = 0; column < W.columns(); column++) {
                float value = 0F;
                int cursor = 0;
                for (int index = 0; index < input.columns(); index++) {
                    value += W.getFloat(cursor + input.getInt(row, index), column);
                    cursor += dimensionSizes[index];
                }
                ret.put(row, column, value);
            }
        }

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
        weightGrad.assign(0F);
        for (int index = 0; index < input.rows(); index++) {
            for (int column = 0; column < delta.columns(); column++) {
                int cursor = 0;
                for (int dimension = 0; dimension < dimensionSizes.length; dimension++) {
                    int point = cursor + input.getInt(index, dimension);
                    float value = weightGrad.getFloat(point, column);
                    value += delta.getFloat(index, column);
                    weightGrad.put(point, column, value);
                    cursor += dimensionSizes[dimension];
                }
            }
        }
        ret.gradientForVariable().put(DefaultParamInitializer.WEIGHT_KEY, weightGrad);

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
