package com.jstarcraft.rns.model.neuralnetwork;

import java.util.HashMap;
import java.util.Map;

import org.nd4j.linalg.factory.Nd4j;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.MathCache;
import com.jstarcraft.ai.math.structure.Nd4jCache;
import com.jstarcraft.ai.math.structure.matrix.MathMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.matrix.Nd4jMatrix;
import com.jstarcraft.ai.model.neuralnetwork.Graph;
import com.jstarcraft.ai.model.neuralnetwork.GraphConfigurator;
import com.jstarcraft.ai.model.neuralnetwork.activation.IdentityActivationFunction;
import com.jstarcraft.ai.model.neuralnetwork.activation.SigmoidActivationFunction;
import com.jstarcraft.ai.model.neuralnetwork.layer.Layer;
import com.jstarcraft.ai.model.neuralnetwork.layer.ParameterConfigurator;
import com.jstarcraft.ai.model.neuralnetwork.layer.WeightLayer;
import com.jstarcraft.ai.model.neuralnetwork.learn.NesterovLearner;
import com.jstarcraft.ai.model.neuralnetwork.normalization.IgnoreNormalizer;
import com.jstarcraft.ai.model.neuralnetwork.optimization.StochasticGradientOptimizer;
import com.jstarcraft.ai.model.neuralnetwork.parameter.XavierUniformParameterFactory;
import com.jstarcraft.ai.model.neuralnetwork.schedule.ConstantSchedule;
import com.jstarcraft.ai.model.neuralnetwork.vertex.LayerVertex;
import com.jstarcraft.core.common.configuration.Configurator;
import com.jstarcraft.rns.model.EpocheModel;

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
public class AutoRecModel extends EpocheModel {

    /**
     * the dimension of input units
     */
    protected int inputDimension;

    /**
     * the dimension of hidden units
     */
    protected int hiddenDimension;

    /**
     * the activation function of the hidden layer in the neural network
     */
    protected String hiddenActivation;

    /**
     * the activation function of the output layer in the neural network
     */
    protected String outputActivation;

    /**
     * the learning rate of the optimization algorithm
     */
    protected float learnRatio;

    /**
     * the momentum of the optimization algorithm
     */
    protected float momentum;

    /**
     * the regularization coefficient of the weights in the neural network
     */
    protected float weightRegularization;

    /**
     * the data structure that stores the training data
     */
    protected Nd4jMatrix inputData;

    /**
     * the data structure that stores the predicted data
     */
    protected Nd4jMatrix outputData;

    protected Graph network;

    /**
     * the data structure that indicates which element in the user-item is non-zero
     */
    private Nd4jMatrix maskData;

    protected int getInputDimension() {
        return userSize;
    }

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        inputDimension = getInputDimension();
        hiddenDimension = configuration.getInteger("recommender.hidden.dimension");
        hiddenActivation = configuration.getString("recommender.hidden.activation");
        outputActivation = configuration.getString("recommender.output.activation");
        learnRatio = configuration.getFloat("recommender.iterator.learnrate");
        momentum = configuration.getFloat("recommender.iterator.momentum");
        weightRegularization = configuration.getFloat("recommender.weight.regularization");

        // transform the sparse matrix to INDArray
        int[] matrixShape = new int[] { itemSize, userSize };
        inputData = new Nd4jMatrix(Nd4j.zeros(matrixShape));
        maskData = new Nd4jMatrix(Nd4j.zeros(matrixShape));
        outputData = new Nd4jMatrix(Nd4j.zeros(matrixShape));
        for (MatrixScalar term : scoreMatrix) {
            if (term.getValue() > 0D) {
                inputData.setValue(term.getColumn(), term.getRow(), term.getValue());
                maskData.setValue(term.getColumn(), term.getRow(), 1F);
            }
        }
    }

    protected Graph getComputationGraph() {
        GraphConfigurator configurator = new GraphConfigurator();
        Map<String, ParameterConfigurator> configurators = new HashMap<>();
        Nd4j.getRandom().setSeed(6L);
        ParameterConfigurator parameterConfigurator = new ParameterConfigurator(0F, weightRegularization, new XavierUniformParameterFactory());
        configurators.put(WeightLayer.WEIGHT_KEY, parameterConfigurator);
        configurators.put(WeightLayer.BIAS_KEY, new ParameterConfigurator(0F, 0F));
        MathCache factory = new Nd4jCache();
        Layer cdaeLayer = new WeightLayer(inputDimension, hiddenDimension, factory, configurators, new SigmoidActivationFunction());
        Layer outputLayer = new WeightLayer(hiddenDimension, inputDimension, factory, configurators, new IdentityActivationFunction());

        configurator.connect(new LayerVertex("input", factory, cdaeLayer, new NesterovLearner(new ConstantSchedule(learnRatio), new ConstantSchedule(momentum)), new IgnoreNormalizer()));
        configurator.connect(new LayerVertex("output", factory, outputLayer, new NesterovLearner(new ConstantSchedule(learnRatio), new ConstantSchedule(momentum)), new IgnoreNormalizer()), "input");

        Graph graph = new Graph(configurator, new StochasticGradientOptimizer(), new AutoRecLossFunction(maskData));
        return graph;
    }

    @Override
    protected void doPractice() {
        Graph graph = getComputationGraph();
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            totalError = graph.practice(1, new MathMatrix[] { inputData }, new MathMatrix[] { inputData });
            if (isConverged(epocheIndex) && isConverged) {
                break;
            }
            currentError = totalError;
        }
        graph.predict(new MathMatrix[] { inputData }, new MathMatrix[] { outputData });
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        instance.setQuantityMark(outputData.getValue(itemIndex, userIndex));
    }
}
