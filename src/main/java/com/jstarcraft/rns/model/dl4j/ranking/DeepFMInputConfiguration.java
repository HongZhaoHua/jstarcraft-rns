package com.jstarcraft.rns.model.dl4j.ranking;

import java.util.Collection;
import java.util.Map;

import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.ParamInitializer;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.FeedForwardLayer;
import org.deeplearning4j.nn.conf.memory.LayerMemoryReport;
import org.deeplearning4j.nn.conf.memory.MemoryReport;
import org.deeplearning4j.optimize.api.TrainingListener;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * 
 * DeepFM输入配置
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
public class DeepFMInputConfiguration extends FeedForwardLayer {

    private int[] dimensionSizes;

    private DeepFMParameter deepFMParameter;

    @Override
    public ParamInitializer initializer() {
        return new DeepFMParameter(dimensionSizes);
    }

    public DeepFMInputConfiguration(int[] dimensionSizes) {
        this.deepFMParameter = new DeepFMParameter(dimensionSizes);
        this.dimensionSizes = dimensionSizes;
    }

    private DeepFMInputConfiguration(Builder builder) {
        super(builder);
        this.deepFMParameter = new DeepFMParameter(builder.dimensionSizes);
        this.dimensionSizes = builder.dimensionSizes;
    }

    @Override
    public Layer instantiate(NeuralNetConfiguration configuration, Collection<TrainingListener> monitors, int layerIndex, INDArray parameters, boolean initialize) {
        DeepFMInputLayer layer = new DeepFMInputLayer(configuration, dimensionSizes);
        layer.setListeners(monitors);
        layer.setIndex(layerIndex);
        layer.setParamsViewArray(parameters);
        Map<String, INDArray> table = initializer().init(configuration, parameters, initialize);
        layer.setParamTable(table);
        layer.setConf(configuration);
        return layer;
    }

    public static class Builder extends FeedForwardLayer.Builder<Builder> {

        private int[] dimensionSizes;

        public Builder(int[] dimensionSizes) {
            this.dimensionSizes = dimensionSizes;
        }

        @Override
        public DeepFMInputConfiguration build() {
            return new DeepFMInputConfiguration(this);
        }

    }

    @Override
    public LayerMemoryReport getMemoryReport(InputType inputType) {
        LayerMemoryReport.Builder builder = new LayerMemoryReport.Builder(layerName, DeepFMInputConfiguration.class, inputType, inputType);
        builder.standardMemory(0, 0).workingMemory(0, 0, 0, 0).cacheMemory(MemoryReport.CACHE_MODE_ALL_ZEROS, MemoryReport.CACHE_MODE_ALL_ZEROS);
        return builder.build();
    }

}
