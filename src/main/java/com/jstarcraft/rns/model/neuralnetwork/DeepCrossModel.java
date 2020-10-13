package com.jstarcraft.rns.model.neuralnetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.nd4j.linalg.factory.Nd4j;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.processor.DataSorter;
import com.jstarcraft.ai.data.processor.DataSplitter;
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
import com.jstarcraft.ai.model.neuralnetwork.vertex.ShareVertex;
import com.jstarcraft.ai.model.neuralnetwork.vertex.accumulation.OuterProductVertex;
import com.jstarcraft.ai.model.neuralnetwork.vertex.operation.PlusVertex;
import com.jstarcraft.ai.model.neuralnetwork.vertex.operation.ShiftVertex;
import com.jstarcraft.ai.model.neuralnetwork.vertex.transformation.HorizontalAttachVertex;
import com.jstarcraft.core.common.option.Option;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.data.processor.AllFeatureDataSorter;
import com.jstarcraft.rns.data.processor.QualityFeatureDataSplitter;
import com.jstarcraft.rns.model.EpocheModel;

/**
 * DCN推荐器
 * 
 * <pre>
 * DCN——Deep & Cross Network for Ad Click Prediction
 * </pre>
 */
public class DeepCrossModel extends EpocheModel {
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
     * 所有维度的特征总数
     */
    private int numberOfFeatures;

    /**
     * the data structure that stores the training data N个样本 f个filed
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

    protected int[] dimensionSizes;

    protected DataModule marker;

    @Override
    public void prepare(Option configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        learnRatio = configuration.getFloat("recommender.iterator.learnrate");
        momentum = configuration.getFloat("recommender.iterator.momentum");
        weightRegularization = configuration.getFloat("recommender.weight.regularization");
        this.marker = model;

        // TODO 此处需要重构,外部索引与内部索引的映射转换
        dimensionSizes = new int[marker.getQualityOrder()];
        for (int orderIndex = 0, orderSize = marker.getQualityOrder(); orderIndex < orderSize; orderIndex++) {
            Entry<Integer, KeyValue<String, Boolean>> term = marker.getOuterKeyValue(orderIndex);
            dimensionSizes[marker.getQualityInner(term.getValue().getKey())] = space.getQualityAttribute(term.getValue().getKey()).getSize();
        }
    }

    protected Graph getComputationGraph(int[] dimensionSizes) {
        Schedule schedule = new ConstantSchedule(learnRatio);
        GraphConfigurator configurator = new GraphConfigurator();
        Map<String, ParameterConfigurator> configurators = new HashMap<>();
        Nd4j.getRandom().setSeed(6L);
        ParameterConfigurator parameter = new ParameterConfigurator(weightRegularization, 0F, new NormalParameterFactory());
        configurators.put(WeightLayer.WEIGHT_KEY, parameter);
        configurators.put(WeightLayer.BIAS_KEY, new ParameterConfigurator(0F, 0F));
        MathCache factory = new DenseCache();

        // 构建Embed节点

        int numberOfFactors = 10;
        String[] embedVertexNames = new String[dimensionSizes.length];
        for (int fieldIndex = 0; fieldIndex < dimensionSizes.length; fieldIndex++) {
            embedVertexNames[fieldIndex] = "Embed" + fieldIndex;
            Layer embedLayer = new EmbedLayer(dimensionSizes[fieldIndex], numberOfFactors, factory, configurators, new IdentityActivationFunction());
            configurator.connect(new LayerVertex(embedVertexNames[fieldIndex], factory, embedLayer, new SgdLearner(schedule), new IgnoreNormalizer()));
        }

        // 构建Net Input节点
        int numberOfHiddens = 20;
        configurator.connect(new HorizontalAttachVertex("EmbedStack", factory), embedVertexNames);
        configurator.connect(new ShiftVertex("EmbedStack0", factory, 0F), "EmbedStack");
        Layer netLayer = new WeightLayer(dimensionSizes.length * numberOfFactors, numberOfHiddens, factory, configurators, new ReLUActivationFunction());
        configurator.connect(new LayerVertex("NetInput", factory, netLayer, new SgdLearner(schedule), new IgnoreNormalizer()), "EmbedStack");

        // cross net
        // 构建crossNet

        int numberOfCrossLayers = 3;

        for (int crossLayerIndex = 0; crossLayerIndex < numberOfCrossLayers; crossLayerIndex++) {
            if (crossLayerIndex == 0) {
                configurator.connect(new OuterProductVertex("OuterProduct" + crossLayerIndex, factory), "EmbedStack0", "EmbedStack"); // （n,fk*fk)
            } else {
                configurator.connect(new OuterProductVertex("OuterProduct" + crossLayerIndex, factory), "EmbedStack" + crossLayerIndex, "EmbedStack"); // （n,fk*fk)
            }

            // // 水平切割
            // String[] outerProductShare=new String[dimensionSizes.length *
            // numberOfFactors];
            // for(int shareIndex=0;shareIndex<dimensionSizes.length *
            // numberOfFactors;shareIndex++)
            // {
            // int from=shareIndex*dimensionSizes.length * numberOfFactors;
            // int end=(shareIndex+1)*dimensionSizes.length * numberOfFactors;
            // configurator.connect(new
            // HorizontalUnstackVertex("OuterProductShare"+shareIndex+crossLayerIndex,factory,from,end),
            // "OuterProduct"+crossLayerIndex);
            // outerProductShare[shareIndex]="OuterProductShare"+shareIndex+crossLayerIndex;
            // }
            //
            // // 水平堆叠
            // configurator.connect(new
            // HorizontalStackVertex("OuterProductShareStack"+crossLayerIndex,factory),outerProductShare);

            Layer crossLayer = new WeightLayer(dimensionSizes.length * numberOfFactors, 1, factory, configurators, new IdentityActivationFunction());
            configurator.connect(new ShareVertex("OutProduct_cross" + crossLayerIndex, factory, dimensionSizes.length * numberOfFactors, crossLayer), "OuterProduct" + crossLayerIndex); // (n,fk)

            if (crossLayerIndex == 0) {
                configurator.connect(new PlusVertex("EmbedStack" + (crossLayerIndex + 1), factory), "OutProduct_cross" + crossLayerIndex, "EmbedStack"); // (n,fk)
            } else {
                configurator.connect(new PlusVertex("EmbedStack" + (crossLayerIndex + 1), factory), "OutProduct_cross" + crossLayerIndex, "EmbedStack" + crossLayerIndex); // (n,fk)
            }
        }

        // dnn
        int numberOfLayers = 5;
        String currentLayer = "NetInput";
        for (int layerIndex = 0; layerIndex < numberOfLayers; layerIndex++) {
            Layer hiddenLayer = new WeightLayer(numberOfHiddens, numberOfHiddens, factory, configurators, new SigmoidActivationFunction());
            configurator.connect(new LayerVertex("NetHidden" + layerIndex, factory, hiddenLayer, new SgdLearner(schedule), new IgnoreNormalizer()), currentLayer);
            currentLayer = "NetHidden" + layerIndex;
        }

        // 构建Deep Output节点
        configurator.connect(new HorizontalAttachVertex("DeepStack", factory), currentLayer, "EmbedStack" + numberOfCrossLayers);
        Layer deepLayer = new WeightLayer(dimensionSizes.length * numberOfFactors + numberOfHiddens, 1, factory, configurators, new SigmoidActivationFunction());
        configurator.connect(new LayerVertex("DeepOutput", factory, deepLayer, new SgdLearner(schedule), new IgnoreNormalizer()), "DeepStack");

        Graph graph = new Graph(configurator, new StochasticGradientOptimizer(), new BinaryXENTLossFunction(false));
        return graph;
    }

    @Override
    protected void doPractice() {
        DataSplitter splitter = new QualityFeatureDataSplitter(userDimension);
        DataModule[] models = splitter.split(marker, userSize);
        DataSorter sorter = new AllFeatureDataSorter();
        for (int index = 0; index < userSize; index++) {
            models[index] = sorter.sort(models[index]);
        }

        DataInstance instance = marker.getInstance(0);

        int[] positiveKeys = new int[dimensionSizes.length], negativeKeys = new int[dimensionSizes.length];

        graph = getComputationGraph(dimensionSizes);

        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            totalError = 0F;

            // TODO 应该调整为配置项.
            int batchSize = 2000;
            inputData = new DenseMatrix[dimensionSizes.length];
            // inputData[dimensionSizes.length] = DenseMatrix.valueOf(batchSize,
            // dimensionSizes.length);
            for (int index = 0; index < dimensionSizes.length; index++) {
                inputData[index] = DenseMatrix.valueOf(batchSize, 1);
            }
            DenseMatrix labelData = DenseMatrix.valueOf(batchSize, 1);

            for (int batchIndex = 0; batchIndex < batchSize;) {
                // 随机用户
                int userIndex = RandomUtility.randomInteger(userSize);
                SparseVector userVector = scoreMatrix.getRowVector(userIndex);
                if (userVector.getElementSize() == 0 || userVector.getElementSize() == itemSize) {
                    continue;
                }

                DataModule module = models[userIndex];
                instance = module.getInstance(0);
                // 获取正样本
                int positivePosition = RandomUtility.randomInteger(module.getSize());
                instance.setCursor(positivePosition);
                for (int index = 0; index < positiveKeys.length; index++) {
                    positiveKeys[index] = instance.getQualityFeature(index);
                }

                // 获取负样本
                int negativeItemIndex = RandomUtility.randomInteger(itemSize - userVector.getElementSize());
                for (int position = 0, size = userVector.getElementSize(); position < size; position++) {
                    if (negativeItemIndex >= userVector.getIndex(position)) {
                        negativeItemIndex++;
                        continue;
                    }
                    break;
                }
                // TODO 注意,此处为了故意制造负面特征.
                int negativePosition = RandomUtility.randomInteger(module.getSize());
                instance.setCursor(negativePosition);
                for (int index = 0; index < negativeKeys.length; index++) {
                    negativeKeys[index] = instance.getQualityFeature(index);
                }
                negativeKeys[itemDimension] = negativeItemIndex;

                for (int dimension = 0; dimension < dimensionSizes.length; dimension++) {
                    // inputData[dimension].putScalar(batchIndex, 0,
                    // positiveKeys[dimension]);
                    // inputData[dimensionSizes.length].setValue(batchIndex, dimension,
                    // positiveKeys[dimension]);
                    inputData[dimension].setValue(batchIndex, 0, positiveKeys[dimension]);
                }
                labelData.setValue(batchIndex, 0, 1);
                batchIndex++;

                for (int dimension = 0; dimension < dimensionSizes.length; dimension++) {
                    // inputData[dimension].putScalar(batchIndex, 0,
                    // negativeKeys[dimension]);
                    // inputData[dimensionSizes.length].setValue(batchIndex, dimension,
                    // negativeKeys[dimension]);
                    inputData[dimension].setValue(batchIndex, 0, negativeKeys[dimension]);
                }
                labelData.setValue(batchIndex, 0, 0);
                batchIndex++;
            }
            totalError = graph.practice(100, inputData, new DenseMatrix[] { labelData });

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

            if (isConverged(epocheIndex) && isConverged) {
                break;
            }
            currentError = totalError;
        }

        // inputData[dimensionSizes.length] = DenseMatrix.valueOf(numberOfUsers,
        // dimensionSizes.length);
        for (int index = 0; index < dimensionSizes.length; index++) {
            inputData[index] = DenseMatrix.valueOf(userSize, 1);
        }

        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            DataModule model = models[userIndex];
            if (model.getSize() > 0) {
                instance = model.getInstance(model.getSize() - 1);
                for (int dimension = 0; dimension < dimensionSizes.length; dimension++) {
                    if (dimension != itemDimension) {
                        int feature = instance.getQualityFeature(dimension);
                        // inputData[dimension].putScalar(userIndex, 0,
                        // keys[dimension]);
                        // inputData[dimensionSizes.length].setValue(userIndex, dimension, feature);
                        inputData[dimension].setValue(userIndex, 0, feature);
                    }
                }
            } else {
                // inputData[dimensionSizes.length].setValue(userIndex, userDimension,
                // userIndex);
                inputData[userDimension].setValue(userIndex, 0, userIndex);
            }
        }

        DenseMatrix labelData = DenseMatrix.valueOf(userSize, 1);
        outputData = DenseMatrix.valueOf(userSize, itemSize);

        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            // inputData[dimensionSizes.length].getColumnVector(itemDimension).calculate(VectorMapper.constantOf(itemIndex),
            // null, Calculator.SERIAL);
            inputData[itemDimension].setValues(itemIndex);
            graph.predict(inputData, new DenseMatrix[] { labelData });
            outputData.getColumnVector(itemIndex).iterateElement(MathCalculator.SERIAL, (scalar) -> {
                scalar.setValue(labelData.getValue(scalar.getIndex(), 0));
            });
        }
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        float value = outputData.getValue(userIndex, itemIndex);
        instance.setQuantityMark(value);
    }

}
