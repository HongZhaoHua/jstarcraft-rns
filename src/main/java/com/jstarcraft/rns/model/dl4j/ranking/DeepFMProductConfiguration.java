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
 * DeepFM Product配置
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
public class DeepFMProductConfiguration extends GraphVertex {

	public DeepFMProductConfiguration() {
	}

	@Override
	public DeepFMProductConfiguration clone() {
		return new DeepFMProductConfiguration();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof DeepFMProductConfiguration;
	}

	@Override
	public int hashCode() {
		return DeepFMProductConfiguration.class.hashCode();
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
		return 2;
	}

	@Override
	public DeepFMProductVertex instantiate(ComputationGraph graph, String name, int vertexIndex, INDArray paramsView, boolean initializeParams) {
		return new DeepFMProductVertex(graph, name, vertexIndex);
	}

	@Override
	public InputType getOutputType(int layerIndex, InputType... vertexInputs) throws InvalidInputTypeException {
		return InputType.feedForward(1);
	}

	@Override
	public MemoryReport getMemoryReport(InputType... inputTypes) {
		// No working memory in addition to output activations
		return new LayerMemoryReport.Builder(null, DeepFMProductConfiguration.class, inputTypes[0], inputTypes[0]).standardMemory(0, 0).workingMemory(0, 0, 0, 0).cacheMemory(0, 0).build();
	}
}
