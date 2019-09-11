package com.jstarcraft.rns.model.dl4j.ranking;

import java.util.Collection;
import java.util.Map;

import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.ParamInitializer;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.BaseOutputLayer;
import org.deeplearning4j.nn.params.DefaultParamInitializer;
import org.deeplearning4j.optimize.api.TrainingListener;
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
//TODO 存档,以后需要基于DL4J重构.
@Deprecated
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
    public Layer instantiate(NeuralNetConfiguration configuration, Collection<TrainingListener> monitors, int layerIndex, INDArray parameters, boolean initialize) {
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
