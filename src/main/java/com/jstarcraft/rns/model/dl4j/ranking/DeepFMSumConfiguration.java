package com.jstarcraft.rns.model.dl4j.ranking;

import org.deeplearning4j.nn.conf.graph.GraphVertex;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.inputs.InvalidInputTypeException;
import org.deeplearning4j.nn.conf.memory.LayerMemoryReport;
import org.deeplearning4j.nn.conf.memory.MemoryReport;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * 
 * DeepFM Sum配置
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
public class DeepFMSumConfiguration extends GraphVertex {

    public DeepFMSumConfiguration() {
    }

    @Override
    public DeepFMSumConfiguration clone() {
        return new DeepFMSumConfiguration();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DeepFMSumConfiguration;
    }

    @Override
    public int hashCode() {
        return DeepFMSumConfiguration.class.hashCode();
    }

    @Override
    public long numParams(boolean backprop) {
        return 0;
    }

    @Override
    public int minVertexInputs() {
        return 2;
    }

    @Override
    public int maxVertexInputs() {
        return Integer.MAX_VALUE;
    }

    @Override
    public DeepFMSumVertex instantiate(ComputationGraph graph, String name, int vertexIndex, INDArray paramsView, boolean initializeParams) {
        return new DeepFMSumVertex(graph, name, vertexIndex);
    }

    @Override
    public InputType getOutputType(int layerIndex, InputType... vertexInputs) throws InvalidInputTypeException {
        return InputType.feedForward(1);
    }

    @Override
    public MemoryReport getMemoryReport(InputType... inputTypes) {
        // No working memory in addition to output activations
        return new LayerMemoryReport.Builder(null, DeepFMSumConfiguration.class, inputTypes[0], inputTypes[0]).standardMemory(0, 0).workingMemory(0, 0, 0, 0).cacheMemory(0, 0).build();
    }
}
