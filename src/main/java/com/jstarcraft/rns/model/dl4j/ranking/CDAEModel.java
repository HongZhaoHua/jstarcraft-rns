package com.jstarcraft.rns.model.dl4j.ranking;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.rns.model.NeuralNetworkModel;

/**
 * 
 * CDAE推荐器
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
public class CDAEModel extends NeuralNetworkModel {

    /**
     * the threshold to binarize the rating
     */
    private double binarie;

    @Override
    protected int getInputDimension() {
        return itemSize;
    }

    @Override
    protected MultiLayerConfiguration getNetworkConfiguration() {
        NeuralNetConfiguration.ListBuilder factory = new NeuralNetConfiguration.Builder().seed(6)
                // .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                // .gradientNormalizationThreshold(1.0)
                .updater(new Nesterovs(learnRatio, momentum)).weightInit(WeightInit.XAVIER).optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).l2(weightRegularization).list();
        factory.layer(0, new CDAEConfiguration.Builder().nIn(inputDimension).nOut(hiddenDimension).activation(Activation.fromString(hiddenActivation)).setNumUsers(userSize).build());
        factory.layer(1, new OutputLayer.Builder().nIn(hiddenDimension).nOut(inputDimension).lossFunction(LossFunctions.LossFunction.SQUARED_LOSS).activation(Activation.fromString(outputActivation)).build());
        factory.pretrain(false).backprop(true);
        MultiLayerConfiguration configuration = factory.build();
        return configuration;
    }

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        binarie = configuration.getFloat("recommender.binarize.threshold");
        // transform the sparse matrix to INDArray
        // the sparse training matrix has been binarized

        int[] matrixShape = new int[] { userSize, itemSize };
        inputData = Nd4j.zeros(matrixShape);
        for (MatrixScalar term : scoreMatrix) {
            if (term.getValue() > binarie) {
                inputData.putScalar(term.getRow(), term.getColumn(), 1D);
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
