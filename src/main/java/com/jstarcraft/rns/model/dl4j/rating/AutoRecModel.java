package com.jstarcraft.rns.model.dl4j.rating;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Nesterovs;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.core.common.option.Option;
import com.jstarcraft.rns.model.NeuralNetworkModel;

/**
 * 
 * AutoRec学习器
 * 
 * <pre>
 * AutoRec: Autoencoders Meet Collaborative Filtering
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
//TODO 存档,以后需要基于DL4J重构.
@Deprecated
public class AutoRecModel extends NeuralNetworkModel {

    /**
     * the data structure that indicates which element in the user-item is non-zero
     */
    private INDArray maskData;

    @Override
    protected int getInputDimension() {
        return userSize;
    }

    @Override
    protected MultiLayerConfiguration getNetworkConfiguration() {
        NeuralNetConfiguration.ListBuilder factory = new NeuralNetConfiguration.Builder().seed(6).updater(new Nesterovs(learnRatio, momentum)).weightInit(WeightInit.XAVIER_UNIFORM).optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).l2(weightRegularization).list();
        factory.layer(0, new DenseLayer.Builder().nIn(inputDimension).nOut(hiddenDimension).activation(Activation.fromString(hiddenActivation)).build());
        factory.layer(1, new OutputLayer.Builder(new AutoRecLearner(maskData)).nIn(hiddenDimension).nOut(inputDimension).activation(Activation.fromString(outputActivation)).build());
        MultiLayerConfiguration configuration = factory.pretrain(false).backprop(true).build();
        return configuration;
    }

    @Override
    public void prepare(Option configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        // transform the sparse matrix to INDArray
        int[] matrixShape = new int[] { itemSize, userSize };
        inputData = Nd4j.zeros(matrixShape);
        maskData = Nd4j.zeros(matrixShape);
        for (MatrixScalar term : scoreMatrix) {
            if (term.getValue() > 0D) {
                inputData.putScalar(term.getColumn(), term.getRow(), term.getValue());
                maskData.putScalar(term.getColumn(), term.getRow(), 1D);
            }
        }
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        instance.setQuantityMark(outputData.getFloat(itemIndex, userIndex));
    }
}
