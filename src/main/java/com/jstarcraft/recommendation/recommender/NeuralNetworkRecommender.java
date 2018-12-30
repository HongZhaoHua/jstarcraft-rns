package com.jstarcraft.recommendation.recommender;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;

import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.InstanceAccessor;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;

/**
 * 神经网络推荐器
 * 
 * @author Birdy
 *
 */
public abstract class NeuralNetworkRecommender extends ModelRecommender {

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
	protected float learnRate;

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
	protected INDArray inputData;

	/**
	 * the data structure that stores the predicted data
	 */
	protected INDArray outputData;

	protected MultiLayerNetwork network;

	protected abstract int getInputDimension();

	protected abstract MultiLayerConfiguration getNetworkConfiguration();

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, InstanceAccessor model, DataSpace space) {
		super.prepare(configuration, marker, model, space);
		inputDimension = getInputDimension();
		hiddenDimension = configuration.getInteger("rec.hidden.dimension");
		hiddenActivation = configuration.getString("rec.hidden.activation");
		outputActivation = configuration.getString("rec.output.activation");
		learnRate = configuration.getFloat("rec.iterator.learnrate");
		momentum = configuration.getFloat("rec.iterator.momentum");
		weightRegularization = configuration.getFloat("rec.weight.regularization");
	}

	@Override
	protected void doPractice() {
		MultiLayerConfiguration configuration = getNetworkConfiguration();
		network = new MultiLayerNetwork(configuration);
		network.init();
		for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
			totalLoss = 0F;
			network.fit(inputData, inputData);
			totalLoss = (float) network.score();
			if (isConverged(iterationStep) && isConverged) {
				break;
			}
			currentLoss = totalLoss;
		}

		outputData = network.output(inputData);
	}

}
