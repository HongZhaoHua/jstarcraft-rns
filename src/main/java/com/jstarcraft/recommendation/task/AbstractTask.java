package com.jstarcraft.recommendation.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.converter.ArffConverter;
import com.jstarcraft.ai.data.converter.CsvConverter;
import com.jstarcraft.ai.data.converter.DataConverter;
import com.jstarcraft.ai.data.module.ReferenceModule;
import com.jstarcraft.ai.environment.EnvironmentContext;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.utility.Int2FloatKeyValue;
import com.jstarcraft.ai.utility.IntegerArray;
import com.jstarcraft.core.utility.JsonUtility;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.core.utility.ReflectionUtility;
import com.jstarcraft.core.utility.StringUtility;
import com.jstarcraft.core.utility.TypeUtility;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.processor.DataMatcher;
import com.jstarcraft.recommendation.data.processor.DataSorter;
import com.jstarcraft.recommendation.data.splitter.DataSplitter;
import com.jstarcraft.recommendation.data.splitter.GivenDataSplitter;
import com.jstarcraft.recommendation.data.splitter.GivenNumberSplitter;
import com.jstarcraft.recommendation.data.splitter.KFoldCrossValidationSplitter;
import com.jstarcraft.recommendation.data.splitter.LeaveOneCrossValidationSplitter;
import com.jstarcraft.recommendation.data.splitter.RandomSplitter;
import com.jstarcraft.recommendation.data.splitter.RatioSplitter;
import com.jstarcraft.recommendation.evaluator.Evaluator;
import com.jstarcraft.recommendation.exception.RecommendationException;
import com.jstarcraft.recommendation.recommender.Recommender;

/**
 * 抽象任务
 * 
 * @author Birdy
 *
 * @param <T>
 */
public abstract class AbstractTask<T> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected Configuration configuration;

    protected String userField, itemField;

    protected int userDimension, itemDimension, numberOfUsers, numberOfItems;

    protected int[] trainPaginations, trainPositions, testPaginations, testPositions;

    protected DataModule dataMarker, trainMarker, testMarker;

    protected Recommender recommender;

    protected AbstractTask(Class<? extends Recommender> clazz, Configuration configuration) {
        this.configuration = configuration;
        Long seed = configuration.getLong("rec.random.seed");
        if (seed != null) {
            RandomUtility.setSeed(seed);
        }
        this.recommender = (Recommender) ReflectionUtility.getInstance(clazz);
    }

    protected abstract Collection<Evaluator> getEvaluators(SparseMatrix featureMatrix);

    protected abstract T check(int userIndex);

    protected abstract List<Int2FloatKeyValue> recommend(Recommender recommender, int userIndex);

    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private Map<Class<? extends Evaluator>, Int2FloatKeyValue> evaluate(Collection<Evaluator> evaluators, Recommender recommender) {
        Map<Class<? extends Evaluator>, Int2FloatKeyValue[]> values = new HashMap<>();
        for (Evaluator evaluator : evaluators) {
            values.put(evaluator.getClass(), new Int2FloatKeyValue[numberOfUsers]);
        }
        // 按照用户切割任务.
        CountDownLatch latch = new CountDownLatch(numberOfUsers);
        for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
            int index = userIndex;
            executor.submit(() -> {
                try {
                    if (testPaginations[index + 1] - testPaginations[index] != 0) {
                        // 校验集合
                        T checkCollection = check(index);
                        // 推荐列表
                        List<Int2FloatKeyValue> recommendList = recommend(recommender, index);
                        // 测量列表
                        for (Evaluator<T> evaluator : evaluators) {
                            Int2FloatKeyValue[] measures = values.get(evaluator.getClass());
                            Int2FloatKeyValue measure = evaluator.evaluate(checkCollection, recommendList);
                            measures[index] = measure;
                        }
                    }

                } catch (Exception exception) {
                    logger.error("任务异常", exception);
                }
                latch.countDown();
            });
        }
        try {
            latch.await();
        } catch (Exception exception) {
            throw new RecommendationException(exception);
        }

        Map<Class<? extends Evaluator>, Int2FloatKeyValue> measures = new HashMap<>();
        for (Entry<Class<? extends Evaluator>, Int2FloatKeyValue[]> term : values.entrySet()) {
            Int2FloatKeyValue measure = new Int2FloatKeyValue(0, 0F);
            // if (term.getKey() == RecallEvaluator.class) {
            // for (KeyValue<Integer, Double> element : term.getValue()) {
            // if (element == null) {
            // continue;
            // }
            // System.out.println(element.getKey() + " " + element.getValue());
            // }
            // }
            for (Int2FloatKeyValue element : term.getValue()) {
                if (element == null) {
                    continue;
                }
                measure.setKey(measure.getKey() + element.getKey());
                measure.setValue(measure.getValue() + element.getValue());
            }
            measures.put(term.getKey(), measure);
        }
        return measures;
    }

    public Map<String, Float> execute() throws Exception {
        // TODO 数据属性部分
        // 离散属性
        Type dicreteConfiguration = TypeUtility.parameterize(HashMap.class, String.class, Class.class);
        Map<String, Class<?>> dicreteDifinitions = JsonUtility.string2Object(configuration.getString("data.space.attributes.dicrete"), dicreteConfiguration);
        // 连续属性
        Type continuousConfiguration = TypeUtility.parameterize(HashMap.class, String.class, Class.class);
        Map<String, Class<?>> continuousDifinitions = JsonUtility.string2Object(configuration.getString("data.space.attributes.continuous"), continuousConfiguration);

        // 数据空间部分
        DataSpace space = new DataSpace(dicreteDifinitions, continuousDifinitions);

        // TODO 数据模型部分
        ModuleConfigurer[] moduleConfigurers = JsonUtility.string2Object(configuration.getString("data.modules"), ModuleConfigurer[].class);
        for (ModuleConfigurer moduleConfigurer : moduleConfigurers) {
            space.makeDenseModule(moduleConfigurer.getName(), moduleConfigurer.getConfiguration(), 1000000000);
        }

        // TODO 数据转换器部分
        Type convertorConfiguration = TypeUtility.parameterize(LinkedHashMap.class, String.class, TypeUtility.parameterize(KeyValue.class, String.class, HashMap.class));
        ConverterConfigurer[] converterConfigurers = JsonUtility.string2Object(configuration.getString("data.converters"), ConverterConfigurer[].class);
        for (ConverterConfigurer converterConfigurer : converterConfigurers) {
            String name = converterConfigurer.getName();
            String type = converterConfigurer.getType();
            String path = converterConfigurer.getPath();
            DataConverter convertor = null;
            switch (type) {
            case "arff": {
                convertor = ReflectionUtility.getInstance(ArffConverter.class, space.getQualityAttributes(), space.getQuantityAttributes());
                break;
            }
            case "csv": {
                convertor = ReflectionUtility.getInstance(CsvConverter.class, configuration.getCharacter("data.splitter.delimiter", ' '), space.getQualityAttributes(), space.getQuantityAttributes());
                break;
            }
            default: {
                throw new RecommendationException("不支持的转换格式");
            }
            }
            File file = new File(path);
            DataModule module = space.getModule(name);
            try (InputStream stream = new FileInputStream(file)) {
                convertor.convert(module, stream, converterConfigurer.getQualityMarkOrder(), converterConfigurer.getQuantityMarkOrder(), converterConfigurer.getWeightOrder());
            }
        }

        // TODO 数据切割器部分
        SplitterConfigurer splitterConfigurer = JsonUtility.string2Object(configuration.getString("data.splitter"), SplitterConfigurer.class);
        DataModule model = space.getModule(splitterConfigurer.getName());
        DataSplitter splitter;
        switch (splitterConfigurer.getType()) {
        case "kcv": {
            int size = configuration.getInteger("data.splitter.kcv.number", 1);
            splitter = new KFoldCrossValidationSplitter(model, size);
            break;
        }
        case "loocv": {
            splitter = new LeaveOneCrossValidationSplitter(space, model, splitterConfigurer.getMatchField(), splitterConfigurer.getSortField());
            break;
        }
        case "testset": {
            int threshold = configuration.getInteger("data.splitter.threshold");
            splitter = new GivenDataSplitter(model, threshold);
            break;
        }
        case "givenn": {
            int number = configuration.getInteger("data.splitter.given-number.number");
            splitter = new GivenNumberSplitter(space, model, splitterConfigurer.getMatchField(), splitterConfigurer.getSortField(), number);
            break;
        }
        case "random": {
            double random = configuration.getDouble("data.splitter.random.value", 0.8D);
            splitter = new RandomSplitter(space, model, splitterConfigurer.getMatchField(), random);
            break;
        }
        case "ratio": {
            double ratio = configuration.getDouble("data.splitter.ratio.value", 0.8D);
            splitter = new RatioSplitter(space, model, splitterConfigurer.getMatchField(), splitterConfigurer.getSortField(), ratio);
            break;
        }
        default: {
            throw new RecommendationException("不支持的划分类型");
        }
        }

        // 评估部分
        userField = configuration.getString("data.model.fields.user", "user");
        itemField = configuration.getString("data.model.fields.item", "item");

        Double binarize = configuration.getDouble("data.convert.binarize.threshold");
        Map<String, Float> measures = new TreeMap<>();

        EnvironmentContext context = Nd4j.getAffinityManager().getClass().getSimpleName().equals("CpuAffinityManager") ? EnvironmentContext.CPU : EnvironmentContext.GPU;
        Future<?> task = context.doTask(() -> {
            try {
                for (int index = 0; index < splitter.getSize(); index++) {
                    IntegerArray trainReference = splitter.getTrainReference(index);
                    IntegerArray testReference = splitter.getTestReference(index);
                    trainMarker = new ReferenceModule(trainReference, model);
                    testMarker = new ReferenceModule(testReference, model);

                    IntegerArray positions = new IntegerArray();
                    for (int position = 0, size = model.getSize(); position < size; position++) {
                        positions.associateData(position);
                    }
                    dataMarker = new ReferenceModule(positions, model);

                    userDimension = model.getQualityInner(userField);
                    itemDimension = model.getQualityInner(itemField);
                    numberOfUsers = space.getQualityAttribute(userField).getSize();
                    numberOfItems = space.getQualityAttribute(itemField).getSize();

                    trainPaginations = new int[numberOfUsers + 1];
                    trainPositions = new int[trainMarker.getSize()];
                    for (int position = 0, size = trainMarker.getSize(); position < size; position++) {
                        trainPositions[position] = position;
                    }
                    DataMatcher trainMatcher = DataMatcher.discreteOf(trainMarker, userDimension);
                    trainMatcher.match(trainPaginations, trainPositions);

                    testPaginations = new int[numberOfUsers + 1];
                    testPositions = new int[testMarker.getSize()];
                    for (int position = 0, size = testMarker.getSize(); position < size; position++) {
                        testPositions[position] = position;
                    }
                    DataMatcher testMatcher = DataMatcher.discreteOf(testMarker, userDimension);
                    testMatcher.match(testPaginations, testPositions);

                    int[] dataPaginations = new int[numberOfUsers + 1];
                    int[] dataPositions = new int[dataMarker.getSize()];
                    for (int position = 0; position < dataMarker.getSize(); position++) {
                        dataPositions[position] = position;
                    }
                    DataMatcher dataMatcher = DataMatcher.discreteOf(dataMarker, userDimension);
                    dataMatcher.match(dataPaginations, dataPositions);
                    DataSorter dataSorter = DataSorter.featureOf(dataMarker);
                    dataSorter.sort(dataPaginations, dataPositions);
                    Table<Integer, Integer, Float> dataTable = HashBasedTable.create();
                    for (DataInstance instance : dataMarker) {
                        int rowIndex = instance.getQualityFeature(userDimension);
                        int columnIndex = instance.getQualityFeature(itemDimension);
                        // TODO 处理冲突
                        dataTable.put(rowIndex, columnIndex, instance.getQuantityMark());
                    }
                    SparseMatrix featureMatrix = SparseMatrix.valueOf(numberOfUsers, numberOfItems, dataTable);

                    recommender.prepare(configuration, trainMarker, space);
                    recommender.practice();
                    for (Entry<Class<? extends Evaluator>, Int2FloatKeyValue> measure : evaluate(getEvaluators(featureMatrix), recommender).entrySet()) {
                        Float value = measure.getValue().getValue() / measure.getValue().getKey();
                        measures.put(measure.getKey().getSimpleName(), value);
                    }
                }
            } catch (Exception exception) {
                logger.error("任务异常", exception);
            }
        });
        task.get();

        for (Entry<String, Float> term : measures.entrySet()) {
            term.setValue(term.getValue() / splitter.getSize());
            if (logger.isInfoEnabled()) {
                logger.info(StringUtility.format("measure of {} is {}", term.getKey(), term.getValue()));
            }
        }
        return measures;
    }

    public Recommender getRecommender() {
        return recommender;
    }

    public DataModule getDataMarker() {
        return dataMarker;
    }

}
