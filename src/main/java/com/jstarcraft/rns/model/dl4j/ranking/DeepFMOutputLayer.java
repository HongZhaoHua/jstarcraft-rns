package com.jstarcraft.rns.model.dl4j.ranking;

import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.gradient.DefaultGradient;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.layers.BaseOutputLayer;
import org.deeplearning4j.nn.params.DefaultParamInitializer;
import org.deeplearning4j.nn.workspace.ArrayType;
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.ILossFunction;
import org.nd4j.linalg.primitives.Pair;

/**
 * 
 * DeepFM输出层
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
public class DeepFMOutputLayer extends BaseOutputLayer<DeepFMOutputConfiguration> {

    public DeepFMOutputLayer(NeuralNetConfiguration configuration) {
        super(configuration);
    }

    private Pair<Gradient, INDArray> getGradientsAndDelta(INDArray output, LayerWorkspaceMgr workspaceMgr) {
        ILossFunction lossFunction = layerConf().getLossFn();
        INDArray label = getLabels2d(workspaceMgr, ArrayType.FF_WORKING_MEM);
        INDArray delta = lossFunction.computeGradient(label, output, layerConf().getActivationFn(), maskArray);
        Gradient gradient = new DefaultGradient();
        INDArray weight = gradientViews.get(DefaultParamInitializer.WEIGHT_KEY);
        INDArray bias = gradientViews.get(DefaultParamInitializer.BIAS_KEY);
        Nd4j.gemm(input, delta, weight, true, false, 1D, 0D);
        delta.sum(bias, 0);
        gradient.gradientForVariable().put(DefaultParamInitializer.WEIGHT_KEY, weight);
        gradient.gradientForVariable().put(DefaultParamInitializer.BIAS_KEY, bias);
        return new Pair<>(gradient, delta);
    }

    @Override
    public Pair<Gradient, INDArray> backpropGradient(INDArray previous, LayerWorkspaceMgr workspaceMgr) {
        INDArray output = preOutput2d(true, workspaceMgr);
        Pair<Gradient, INDArray> keyValue = getGradientsAndDelta(output, workspaceMgr);
        INDArray delta = keyValue.getValue();
        INDArray next = params.get(DefaultParamInitializer.WEIGHT_KEY).mmul(delta.transpose()).transpose();
        return new Pair<>(keyValue.getKey(), next);
    }

    @Override
    protected INDArray getLabels2d(LayerWorkspaceMgr workspaceMgr, ArrayType arrayType) {
        return labels;
    }

}
