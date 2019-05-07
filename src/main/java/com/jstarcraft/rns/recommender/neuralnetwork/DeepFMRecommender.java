package com.jstarcraft.rns.recommender.neuralnetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.nd4j.linalg.factory.Nd4j;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DenseCache;
import com.jstarcraft.ai.math.structure.MathCache;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.model.neuralnetwork.Graph;
import com.jstarcraft.ai.model.neuralnetwork.GraphConfigurator;
import com.jstarcraft.ai.model.neuralnetwork.activation.IdentityActivationFunction;
import com.jstarcraft.ai.model.neuralnetwork.activation.ReLUActivationFunction;
import com.jstarcraft.ai.model.neuralnetwork.activation.SigmoidActivationFunction;
import com.jstarcraft.ai.model.neuralnetwork.layer.EmbedLayer;
import com.jstarcraft.ai.model.neuralnetwork.layer.Layer;
import com.jstarcraft.ai.model.neuralnetwork.layer.ParameterConfigurator;
import com.jstarcraft.ai.model.neuralnetwork.layer.WeightLayer;
import com.jstarcraft.ai.model.neuralnetwork.learn.SgdLearner;
import com.jstarcraft.ai.model.neuralnetwork.loss.BinaryXENTLossFunction;
import com.jstarcraft.ai.model.neuralnetwork.normalization.IgnoreNormalizer;
import com.jstarcraft.ai.model.neuralnetwork.optimization.StochasticGradientOptimizer;
import com.jstarcraft.ai.model.neuralnetwork.parameter.NormalParameterFactory;
import com.jstarcraft.ai.model.neuralnetwork.schedule.ConstantSchedule;
import com.jstarcraft.ai.model.neuralnetwork.schedule.Schedule;
import com.jstarcraft.ai.model.neuralnetwork.vertex.LayerVertex;
import com.jstarcraft.ai.model.neuralnetwork.vertex.accumulation.InnerProductVertex;
import com.jstarcraft.ai.model.neuralnetwork.vertex.transformation.HorizontalAttachVertex;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.configurator.Configuration;
import com.jstarcraft.rns.recommender.ModelRecommender;

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
    protected DenseMatrix[] inputData;

    /**
     * the data structure that stores the predicted data
     */
    protected DenseMatrix outputData;

    /**
     * 计算图
     */
    protected Graph graph;

    protected DataModule marker;

    protected int[] dimensionSizes;

    @Override
    public void prepare(Configuration configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        learnRate = configuration.getFloat("rec.iterator.learnrate");
        momentum = configuration.getFloat("rec.iterator.momentum");
        weightRegularization = configuration.getFloat("rec.weight.regularization");
        this.marker = model;

        // TODO 此处需要重构,外部索引与内部索引的映射转换
        dimensionSizes = new int[marker.getQualityOrder()];
        for (int orderIndex = 0, orderSize = marker.getQualityOrder(); orderIndex < orderSize; orderIndex++) {
            Entry<Integer, KeyValue<String, Boolean>> term = marker.getOuterKeyValue(orderIndex);
            dimensionSizes[marker.getQualityInner(term.getValue().getKey())] = space.getQualityAttribute(term.getValue().getKey()).getSize();
        }
    }

    protected Graph getComputationGraph(int[] dimensionSizes) {
        Schedule schedule = new ConstantSchedule(learnRate);
        GraphConfigurator configurator = new GraphConfigurator();
        Map<String, ParameterConfigurator> configurators = new HashMap<>();
        Nd4j.getRandom().setSeed(6L);
        ParameterConfigurator parameter = new ParameterConfigurator(weightRegularization, 0F, new NormalParameterFactory());
        configurators.put(WeightLayer.WEIGHT_KEY, parameter);
        configurators.put(WeightLayer.BIAS_KEY, new ParameterConfigurator(0F, 0F));
        MathCache factory = new DenseCache();

        // 构建Embed节点
        // TODO 应该调整为配置项.
        int numberOfFactors = 10;
        // TODO Embed只支持输入的column为1.
        String[] embedVertexNames = new String[dimensionSizes.length];
        for (int fieldIndex = 0; fieldIndex < dimensionSizes.length; fieldIndex++) {
            embedVertexNames[fieldIndex] = "Embed" + fieldIndex;
            Layer embedLayer = new EmbedLayer(dimensionSizes[fieldIndex], numberOfFactors, factory, configurators, new IdentityActivationFunction());
            configurator.connect(new LayerVertex(embedVertexNames[fieldIndex], factory, embedLayer, new SgdLearner(schedule), new IgnoreNormalizer()));
        }

        // 构建因子分解机部分
        // 构建FM Plus节点(实际就是FM的输入)
        numberOfFeatures = 0;
        for (int fieldIndex = 0; fieldIndex < dimensionSizes.length; fieldIndex++) {
            numberOfFeatures += dimensionSizes[fieldIndex];
        }
        // TODO 注意,由于EmbedLayer不支持与其它Layer共享输入,所以FM Plus节点构建自己的One Hot输入.
        Layer fmLayer = new FMLayer(dimensionSizes, numberOfFeatures, 1, factory, configurators, new IdentityActivationFunction());
        configurator.connect(new LayerVertex("FMPlus", factory, fmLayer, new SgdLearner(schedule), new IgnoreNormalizer()));

        // 构建FM Product节点
        // 注意:节点数量是(n*(n-1)/2)),n为Embed节点数量
        String[] productVertexNames = new String[dimensionSizes.length * (dimensionSizes.length - 1) / 2];
        int productIndex = 0;
        for (int outterFieldIndex = 0; outterFieldIndex < dimensionSizes.length; outterFieldIndex++) {
            for (int innerFieldIndex = outterFieldIndex + 1; innerFieldIndex < dimensionSizes.length; innerFieldIndex++) {
                productVertexNames[productIndex] = "FMProduct" + outterFieldIndex + ":" + innerFieldIndex;
                String left = embedVertexNames[outterFieldIndex];
                String right = embedVertexNames[innerFieldIndex];
                configurator.connect(new InnerProductVertex(productVertexNames[productIndex], factory), left, right);
                productIndex++;
            }
        }

        // 构建FM Sum节点(实际就是FM的输出)
        String[] names = new String[productVertexNames.length + 2];
        System.arraycopy(productVertexNames, 0, names, 0, productVertexNames.length);
        names[productVertexNames.length] = "FMPlus";
        // configurator.connect(new SumVertex("FMOutput"), names);

        // 构建多层网络部分
        // 构建Net Input节点
        // TODO 调整为支持输入(连续域)Dense Field.
        // TODO 应该调整为配置项.
        int numberOfHiddens = 20;
        configurator.connect(new HorizontalAttachVertex("EmbedStack", factory), embedVertexNames);
        Layer netLayer = new WeightLayer(dimensionSizes.length * numberOfFactors, numberOfHiddens, factory, configurators, new ReLUActivationFunction());
        configurator.connect(new LayerVertex("NetInput", factory, netLayer, new SgdLearner(schedule), new IgnoreNormalizer()), "EmbedStack");

        // TODO 应该调整为配置项.
        int numberOfLayers = 5;
        String currentLayer = "NetInput";
        for (int layerIndex = 0; layerIndex < numberOfLayers; layerIndex++) {
            Layer hiddenLayer = new WeightLayer(numberOfHiddens, numberOfHiddens, factory, configurators, new ReLUActivationFunction());
            configurator.connect(new LayerVertex("NetHidden" + layerIndex, factory, hiddenLayer, new SgdLearner(schedule), new IgnoreNormalizer()), currentLayer);
            currentLayer = "NetHidden" + layerIndex;
        }
        names[productVertexNames.length + 1] = currentLayer;

        // 构建Deep Output节点
        configurator.connect(new HorizontalAttachVertex("DeepStack", factory), names);
        Layer deepLayer = new WeightLayer(productVertexNames.length + 1 + numberOfHiddens, 1, factory, configurators, new SigmoidActivationFunction());
        configurator.connect(new LayerVertex("DeepOutput", factory, deepLayer, new SgdLearner(schedule), new IgnoreNormalizer()), "DeepStack");

        Graph graph = new Graph(configurator, new StochasticGradientOptimizer(), new BinaryXENTLossFunction(false));
        return graph;
    }

    @Override
    protected void doPractice() {
        DataInstance instance = marker.getInstance(0);

        int[] positiveKeys = new int[dimensionSizes.length], negativeKeys = new int[dimensionSizes.length];

        graph = getComputationGraph(dimensionSizes);

        for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
            totalLoss = 0F;

            // TODO 应该调整为配置项.
            int batchSize = 2000;
            inputData = new DenseMatrix[dimensionSizes.length + 1];
            inputData[dimensionSizes.length] = DenseMatrix.valueOf(batchSize, dimensionSizes.length);
            for (int index = 0; index < dimensionSizes.length; index++) {
                inputData[index] = DenseMatrix.valueOf(batchSize, 1);
            }
            DenseMatrix labelData = DenseMatrix.valueOf(batchSize, 1);

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
                instance.setCursor(positivePosition);
                for (int index = 0; index < positiveKeys.length; index++) {
                    positiveKeys[index] = instance.getQualityFeature(index);
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
                instance.setCursor(negativePosition);
                for (int index = 0; index < negativeKeys.length; index++) {
                    negativeKeys[index] = instance.getQualityFeature(index);
                }
                negativeKeys[itemDimension] = negativeItemIndex;

                for (int dimension = 0; dimension < dimensionSizes.length; dimension++) {
                    // inputData[dimension].putScalar(batchIndex, 0,
                    // positiveKeys[dimension]);
                    inputData[dimensionSizes.length].setValue(batchIndex, dimension, positiveKeys[dimension]);
                    inputData[dimension].setValue(batchIndex, 0, positiveKeys[dimension]);
                }
                labelData.setValue(batchIndex, 0, 1);
                batchIndex++;

                for (int dimension = 0; dimension < dimensionSizes.length; dimension++) {
                    // inputData[dimension].putScalar(batchIndex, 0,
                    // negativeKeys[dimension]);
                    inputData[dimensionSizes.length].setValue(batchIndex, dimension, negativeKeys[dimension]);
                    inputData[dimension].setValue(batchIndex, 0, negativeKeys[dimension]);
                }
                labelData.setValue(batchIndex, 0, 0);
                batchIndex++;
            }
            totalLoss = graph.practice(100, inputData, new DenseMatrix[] { labelData });

            DenseMatrix[] data = new DenseMatrix[inputData.length];
            DenseMatrix label = DenseMatrix.valueOf(10, 1);
            for (int index = 0; index < data.length; index++) {
                DenseMatrix input = inputData[index];
                data[index] = DenseMatrix.valueOf(10, input.getColumnSize());
                data[index].iterateElement(MathCalculator.SERIAL, (scalar) -> {
                    scalar.setValue(input.getValue(scalar.getRow(), scalar.getColumn()));
                });
            }
            graph.predict(data, new DenseMatrix[] { label });
            System.out.println(label);

            if (isConverged(iterationStep) && isConverged) {
                break;
            }
            currentLoss = totalLoss;
        }

        inputData[dimensionSizes.length] = DenseMatrix.valueOf(numberOfUsers, dimensionSizes.length);
        for (int index = 0; index < dimensionSizes.length; index++) {
            inputData[index] = DenseMatrix.valueOf(numberOfUsers, 1);
        }

        for (int dimension = 0; dimension < dimensionSizes.length; dimension++) {
            if (dimension != itemDimension) {
                for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
                    int position = dataPositions[dataPaginations[userIndex + 1] - 1];
                    instance.setCursor(position);
                    int feature = instance.getQualityFeature(dimension);
                    // inputData[dimension].putScalar(userIndex, 0,
                    // keys[dimension]);
                    inputData[dimensionSizes.length].setValue(userIndex, dimension, feature);
                    inputData[dimension].setValue(userIndex, 0, feature);
                }
            }
        }

        DenseMatrix labelData = DenseMatrix.valueOf(numberOfUsers, 1);
        outputData = DenseMatrix.valueOf(numberOfUsers, numberOfItems);

        for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
            inputData[dimensionSizes.length].getColumnVector(itemDimension).setValues(itemIndex);
            inputData[itemDimension].setValues(itemIndex);
            graph.predict(inputData, new DenseMatrix[] { labelData });
            outputData.getColumnVector(itemIndex).iterateElement(MathCalculator.SERIAL, (scalar) -> {
                scalar.setValue(labelData.getValue(scalar.getIndex(), 0));
            });
        }
    }

    @Override
    public float predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        float value = outputData.getValue(userIndex, itemIndex);
        return value;
    }

}
