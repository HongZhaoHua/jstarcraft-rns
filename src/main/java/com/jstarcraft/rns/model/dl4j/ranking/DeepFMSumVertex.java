package com.jstarcraft.rns.model.dl4j.ranking;

import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.MaskState;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.graph.vertex.BaseGraphVertex;
import org.deeplearning4j.nn.graph.vertex.VertexIndices;
import org.deeplearning4j.nn.workspace.ArrayType;
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.transforms.Or;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;

/**
 * 
 * DeepFM Sum节点
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
public class DeepFMSumVertex extends BaseGraphVertex {

    public DeepFMSumVertex(ComputationGraph graph, String name, int vertexIndex) {
        this(graph, name, vertexIndex, null, null);
    }

    public DeepFMSumVertex(ComputationGraph graph, String name, int vertexIndex, VertexIndices[] inputVertices, VertexIndices[] outputVertices) {
        super(graph, name, vertexIndex, inputVertices, outputVertices);
    }

    @Override
    public boolean hasLayer() {
        return false;
    }

    @Override
    public boolean isOutputVertex() {
        return false;
    }

    @Override
    public Layer getLayer() {
        return null;
    }

    @Override
    public INDArray doForward(boolean training, LayerWorkspaceMgr workspaceMgr) {
        if (!canDoForward()) {
            throw new IllegalStateException("Cannot do forward pass: inputs not set");
        }

        // inputs[index] => {batchSize, numberOfEmbeds}
        INDArray output = inputs[0].sum(1);
        // 求N个行向量的总和
        for (int index = 1; index < inputs.length; index++) {
            output.addi(inputs[index].sum(1));
        }
        // output => {batchSize, 1}
        return workspaceMgr.dup(ArrayType.ACTIVATIONS, output);
    }

    @Override
    public Pair<Gradient, INDArray[]> doBackward(boolean tbptt, LayerWorkspaceMgr workspaceMgr) {
        if (!canDoBackward()) {
            throw new IllegalStateException("Cannot do backward pass: errors not set");
        }
        // epsilons[index] => {batchSize, numberOfEmbeds}
        INDArray[] epsilons = new INDArray[inputs.length];
        // epsilon => {batchSize, 1}
        // inputs[index] => {batchSize, numberOfEmbeds}
        // TODO 如何通过inputs[index]与epsilon求导epsilons[index]
        INDArray output = doForward(true, workspaceMgr);
        for (int index = 0; index < inputs.length; index++) {
            epsilons[index] = workspaceMgr.dup(ArrayType.ACTIVATION_GRAD, inputs[index]);
            epsilons[index].muliColumnVector(epsilon).diviColumnVector(output);
        }
        return new Pair<>(null, epsilons);
    }

    @Override
    public void setBackpropGradientsViewArray(INDArray backpropGradientsViewArray) {
        if (backpropGradientsViewArray != null)
            throw new RuntimeException("Vertex does not have gradients; gradients view array cannot be set here");
    }

    @Override
    public Pair<INDArray, MaskState> feedForwardMaskArrays(INDArray[] maskArrays, MaskState currentMaskState, int minibatchSize) {
        if (maskArrays == null) {
            return new Pair<>(null, currentMaskState);
        }

        // Most common case: all or none.
        // If there's only *some* mask arrays: assume the others (missing) are
        // equivalent to all 1s
        // And for handling multiple masks: best strategy seems to be an OR
        // operation
        // i.e., output is 1 if any of the input are 1s
        // Which means: if any masks are missing, output null (equivalent to no
        // mask, or all steps present)
        // Otherwise do an element-wise OR operation

        for (INDArray mask : maskArrays) {
            if (mask == null) {
                return new Pair<>(null, currentMaskState);
            }
        }

        // At this point: all present. Do OR operation
        if (maskArrays.length == 1) {
            return new Pair<>(maskArrays[0], currentMaskState);
        } else {
            INDArray mask = maskArrays[0].dup(maskArrays[0].ordering());
            for (int index = 1; index < maskArrays.length; index++) {
                Nd4j.getExecutioner().exec(new Or(maskArrays[index], mask, mask));
            }
            return new Pair<>(mask, currentMaskState);
        }
    }

    @Override
    public String toString() {
        return "DeepFMActivationVertex(id=" + this.getVertexIndex() + ",name=\"" + this.getVertexName() + "\")";
    }

}