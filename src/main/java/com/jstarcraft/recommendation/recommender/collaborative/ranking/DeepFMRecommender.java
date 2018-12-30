package com.jstarcraft.recommendation.recommender.collaborative.ranking;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration.GraphBuilder;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.EmbeddingLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.InstanceAccessor;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;
import com.jstarcraft.recommendation.recommender.ModelRecommender;

/**
 * 
 * DeepFM推荐器
 * 
 * <pre>
 * DeepFM: A Factorization-Machine based Neural Network for CTR Prediction
 * </pre>
 * 
 * @author Birdy
 *
 */
public class DeepFMRecommender extends ModelRecommender {

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
	 * 所有维度的特征总数
	 */
	private int numberOfFeatures;

	/**
	 * the data structure that stores the training data
	 */
	protected INDArray[] inputData;

	/**
	 * the data structure that stores the predicted data
	 */
	protected INDArray outputData;

	/**
	 * 计算图
	 */
	protected ComputationGraph graph;

	protected SampleAccessor marker;

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, InstanceAccessor model, DataSpace space) {
		super.prepare(configuration, marker, model, space);
		learnRate = configuration.getFloat("rec.iterator.learnrate");
		momentum = configuration.getFloat("rec.iterator.momentum");
		weightRegularization = configuration.getFloat("rec.weight.regularization");
		this.marker = marker;
	}

	/**
	 * 获取计算图配置
	 * 
	 * @param dimensionSizes
	 * @return
	 */
	protected ComputationGraphConfiguration getComputationGraphConfiguration(int[] dimensionSizes) {
		NeuralNetConfiguration.Builder netBuilder = new NeuralNetConfiguration.Builder();
		// 设置随机种子
		netBuilder.seed(6);
		netBuilder.weightInit(WeightInit.XAVIER_UNIFORM);
		netBuilder.updater(new Sgd(learnRate)).optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT);
		netBuilder.l1(weightRegularization);

		GraphBuilder graphBuilder = netBuilder.graphBuilder();

		// 构建离散域(SparseField)节点
		String[] inputVertexNames = new String[dimensionSizes.length];
		int[] inputVertexSizes = new int[dimensionSizes.length];
		for (int fieldIndex = 0; fieldIndex < dimensionSizes.length; fieldIndex++) {
			inputVertexNames[fieldIndex] = "SparseField" + fieldIndex;
			// 每个离散特征的输入数
			inputVertexSizes[fieldIndex] = dimensionSizes[fieldIndex];
		}
		graphBuilder.addInputs(inputVertexNames);

		// 构建Embed节点
		// TODO 应该调整为配置项.
		int numberOfFactors = 10;
		// TODO Embed只支持输入的column为1.
		String[] embedVertexNames = new String[dimensionSizes.length];
		for (int fieldIndex = 0; fieldIndex < dimensionSizes.length; fieldIndex++) {
			embedVertexNames[fieldIndex] = "Embed" + fieldIndex;
			graphBuilder.addLayer(embedVertexNames[fieldIndex], new EmbeddingLayer.Builder().nIn(inputVertexSizes[fieldIndex]).nOut(numberOfFactors).activation(Activation.IDENTITY).build(), inputVertexNames[fieldIndex]);
		}

		// 构建因子分解机部分
		// 构建FM Plus节点(实际就是FM的输入)
		numberOfFeatures = 0;
		for (int fieldIndex = 0; fieldIndex < dimensionSizes.length; fieldIndex++) {
			numberOfFeatures += inputVertexSizes[fieldIndex];
		}
		// TODO 注意,由于EmbedLayer不支持与其它Layer共享输入,所以FM Plus节点构建自己的One Hot输入.
		graphBuilder.addInputs("FMInputs");
		graphBuilder.addLayer("FMPlus", new DeepFMInputConfiguration.Builder(dimensionSizes).nOut(1).activation(Activation.IDENTITY).build(), "FMInputs");

		// 构建FM Product节点
		// 注意:节点数量是(n*(n-1)/2)),n为Embed节点数量
		String[] productVertexNames = new String[dimensionSizes.length * (dimensionSizes.length - 1) / 2];
		int productIndex = 0;
		for (int outterFieldIndex = 0; outterFieldIndex < dimensionSizes.length; outterFieldIndex++) {
			for (int innerFieldIndex = outterFieldIndex + 1; innerFieldIndex < dimensionSizes.length; innerFieldIndex++) {
				productVertexNames[productIndex] = "FMProduct" + outterFieldIndex + ":" + innerFieldIndex;
				String left = embedVertexNames[outterFieldIndex];
				String right = embedVertexNames[innerFieldIndex];
				graphBuilder.addVertex(productVertexNames[productIndex], new DeepFMProductConfiguration(), left, right);
				productIndex++;
			}
		}

		// 构建FM Sum节点(实际就是FM的输出)
		String[] names = new String[productVertexNames.length + 1];
		System.arraycopy(productVertexNames, 0, names, 0, productVertexNames.length);
		names[names.length - 1] = "FMPlus";
		graphBuilder.addVertex("FMOutput", new DeepFMSumConfiguration(), names);

		// 构建多层网络部分
		// 构建Net Input节点
		// TODO 调整为支持输入(连续域)Dense Field.
		// TODO 应该调整为配置项.
		int numberOfHiddens = 100;
		graphBuilder.addLayer("NetInput", new DenseLayer.Builder().nIn(dimensionSizes.length * numberOfFactors).nOut(numberOfHiddens).activation(Activation.LEAKYRELU).build(), embedVertexNames);

		// TODO 应该调整为配置项.
		int numberOfLayers = 5;
		String currentLayer = "NetInput";
		for (int layerIndex = 0; layerIndex < numberOfLayers; layerIndex++) {
			graphBuilder.addLayer("NetHidden" + layerIndex, new DenseLayer.Builder().nIn(numberOfHiddens).nOut(numberOfHiddens).activation(Activation.LEAKYRELU).build(), currentLayer);
			currentLayer = "NetHidden" + layerIndex;
		}

		// 构建Net Output节点
		graphBuilder.addVertex("NetOutput", new DeepFMSumConfiguration(), currentLayer);

		// 构建Deep Output节点
		graphBuilder.addLayer("DeepOutput", new DeepFMOutputConfiguration.Builder(LossFunctions.LossFunction.XENT).activation(Activation.SIGMOID).nIn(2).nOut(1).build(), "FMOutput", "NetOutput");

		graphBuilder.setOutputs("DeepOutput");
		ComputationGraphConfiguration configuration = graphBuilder.build();
		return configuration;
	}

	@Override
	protected void doPractice() {
		int[] dimensionSizes = new int[marker.getDiscreteOrder()];
		for (int orderIndex = 0; orderIndex < dimensionSizes.length; orderIndex++) {
			dimensionSizes[orderIndex] = marker.getDiscreteAttribute(orderIndex).getSize();
		}
		int[] positiveKeys = new int[dimensionSizes.length], negativeKeys = new int[dimensionSizes.length];

		ComputationGraphConfiguration configuration = getComputationGraphConfiguration(dimensionSizes);

		graph = new ComputationGraph(configuration);
		graph.init();

		for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
			totalLoss = 0F;

			// TODO 应该调整为配置项.
			int batchSize = 2000;
			inputData = new INDArray[dimensionSizes.length + 1];
			inputData[dimensionSizes.length] = Nd4j.zeros(batchSize, dimensionSizes.length);
			for (int index = 0; index < dimensionSizes.length; index++) {
				inputData[index] = inputData[dimensionSizes.length].getColumn(index);
			}
			INDArray labelData = Nd4j.zeros(batchSize, 1);

			for (int batchIndex = 0; batchIndex < batchSize;) {
				// 随机用户
				int userIndex = RandomUtility.randomInteger(numberOfUsers);
				SparseVector userVector = trainMatrix.getRowVector(userIndex);
				if (userVector.getElementSize() == 0 || userVector.getElementSize() == numberOfItems) {
					continue;
				}

				int from = dataPaginations[userIndex], to = dataPaginations[userIndex + 1];
				// 获取正样本
				int positivePosition = dataPositions[RandomUtility.randomInteger(from, to)];
				for (int index = 0; index < positiveKeys.length; index++) {
					positiveKeys[index] = marker.getDiscreteFeature(index, positivePosition);
				}

				// 获取负样本
				int negativeItemIndex = RandomUtility.randomInteger(numberOfItems - userVector.getElementSize());
				for (int position = 0, size = userVector.getElementSize(); position < size; position++) {
					if (negativeItemIndex >= userVector.getIndex(position)) {
						negativeItemIndex++;
						continue;
					}
					break;
				}
				// TODO 注意,此处为了故意制造负面特征.
				int negativePosition = dataPositions[RandomUtility.randomInteger(from, to)];
				for (int index = 0; index < negativeKeys.length; index++) {
					negativeKeys[index] = marker.getDiscreteFeature(index, negativePosition);
				}
				negativeKeys[itemDimension] = negativeItemIndex;

				for (int dimension = 0; dimension < dimensionSizes.length; dimension++) {
					// inputData[dimension].putScalar(batchIndex, 0,
					// positiveKeys[dimension]);
					inputData[dimensionSizes.length].putScalar(batchIndex, dimension, positiveKeys[dimension]);
				}
				labelData.put(batchIndex, 0, 1);
				batchIndex++;

				for (int dimension = 0; dimension < dimensionSizes.length; dimension++) {
					// inputData[dimension].putScalar(batchIndex, 0,
					// negativeKeys[dimension]);
					inputData[dimensionSizes.length].putScalar(batchIndex, dimension, negativeKeys[dimension]);
				}
				labelData.put(batchIndex, 0, 0);
				batchIndex++;
			}
			graph.setInputs(inputData);
			graph.setLabels(labelData);
			for (int iterationIndex = 0; iterationIndex < 100; iterationIndex++) {
				graph.fit();
			}

			INDArray[] data = new INDArray[inputData.length];
			for (int index = 0; index < data.length; index++) {
				data[index] = inputData[index].get(NDArrayIndex.interval(0, 10));
			}
			System.out.println(graph.outputSingle(data));
			totalLoss = (float) graph.score();
			if (isConverged(iterationStep) && isConverged) {
				break;
			}
			currentLoss = totalLoss;
		}

		inputData[dimensionSizes.length] = Nd4j.zeros(numberOfUsers, dimensionSizes.length);
		for (int index = 0; index < dimensionSizes.length; index++) {
			inputData[index] = inputData[dimensionSizes.length].getColumn(index);
		}

		for (int dimension = 0; dimension < dimensionSizes.length; dimension++) {
			if (dimension != itemDimension) {
				for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
					int position = dataPositions[dataPaginations[userIndex + 1] - 1];
					int feature = marker.getDiscreteFeature(dimension, position);
					// inputData[dimension].putScalar(userIndex, 0,
					// keys[dimension]);
					inputData[dimensionSizes.length].putScalar(userIndex, dimension, feature);
				}
			}
		}

		outputData = Nd4j.zeros(numberOfUsers, numberOfItems);

		for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
			inputData[itemDimension].assign(itemIndex);
			outputData.putColumn(itemIndex, graph.outputSingle(inputData));
		}
	}

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		int userIndex = dicreteFeatures[userDimension];
		int itemIndex = dicreteFeatures[itemDimension];
		float value = outputData.getFloat(userIndex, itemIndex);
		return value;
	}

}
