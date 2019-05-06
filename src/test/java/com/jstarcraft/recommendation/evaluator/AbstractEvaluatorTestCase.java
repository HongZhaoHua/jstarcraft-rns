package com.jstarcraft.recommendation.evaluator;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.converter.CsvConverter;
import com.jstarcraft.ai.data.module.ReferenceModule;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.utility.Int2FloatKeyValue;
import com.jstarcraft.ai.utility.IntegerArray;
import com.jstarcraft.recommendation.data.processor.DataMatcher;
import com.jstarcraft.recommendation.data.processor.DataSorter;
import com.jstarcraft.recommendation.data.splitter.DataSplitter;
import com.jstarcraft.recommendation.data.splitter.LeaveOneCrossValidationSplitter;
import com.jstarcraft.recommendation.recommender.Recommender;

public abstract class AbstractEvaluatorTestCase<T> {

    protected String userField, itemField, instantField, scoreField;

    protected int userDimension, itemDimension, instantDimension, numberOfUsers, numberOfItems, numberOfInstants;

    protected int[] trainPaginations, trainPositions, testPaginations, testPositions;

    protected DataModule trainMarker, testMarker;

    protected abstract Evaluator<T> getEvaluator(SparseMatrix featureMatrix);

    protected abstract float getMeasure();

    @Test
    public void test() throws Exception {
        Map<String, Class<?>> discreteFeatures = new HashMap<>();
        Map<String, Class<?>> continuousFeatures = new HashMap<>();
        discreteFeatures.put("user", int.class);
        discreteFeatures.put("item", int.class);
        discreteFeatures.put("instant", long.class);
        DataSpace space = new DataSpace(discreteFeatures, continuousFeatures);

       
        TreeMap<Integer, String> configuration = new TreeMap<>();
        configuration.put(1, "user");
        configuration.put(2, "item");
        configuration.put(3, "instant");
        CsvConverter csvConvertor = new CsvConverter(' ', space.getQualityAttributes(), space.getQuantityAttributes());
       
        // 制造数据模型
        DataModule model = space.makeDenseModule("model", configuration, 1000000);
        
        String path = "data/filmtrust/score.txt";
        File file = new File(path);
        InputStream stream = new FileInputStream(file);
        int count = csvConvertor.convert(model, stream, null, 3, null);

        DataSplitter splitter = new LeaveOneCrossValidationSplitter(space, model, "user", "instant");
        IntegerArray trainReference = splitter.getTrainReference(0);
        IntegerArray testReference = splitter.getTestReference(0);

        userField = "user";
        itemField = "item";
        scoreField = "score";
        instantField = "instant";

        trainMarker = new ReferenceModule(trainReference, model);
        testMarker = new ReferenceModule(testReference, model);
        IntegerArray positions = new IntegerArray();
        for (int index = 0, size = model.getSize(); index < size; index++) {
            positions.associateData(index);
        }
        ReferenceModule dataMarker = new ReferenceModule(positions, model);

        userDimension = model.getQualityInner(userField);
        itemDimension = model.getQualityInner(itemField);
        instantDimension = model.getQualityInner(instantField);
        numberOfUsers = space.getQualityAttribute(userField).getSize();
        numberOfItems = space.getQualityAttribute(itemField).getSize();
        numberOfInstants = space.getQualityAttribute(instantField).getSize();

        trainPaginations = new int[numberOfUsers + 1];
        trainPositions = new int[trainMarker.getSize()];
        for (int index = 0; index < trainMarker.getSize(); index++) {
            trainPositions[index] = index;
        }
        DataMatcher trainMatcher = DataMatcher.discreteOf(trainMarker, userDimension);
        trainMatcher.match(trainPaginations, trainPositions);
        DataSorter trainSorter = DataSorter.featureOf(trainMarker);
        trainSorter.sort(trainPaginations, trainPositions);
        Table<Integer, Integer, Float> trainTable = HashBasedTable.create();
        DataInstance trainInstance = trainMarker.getInstance(0);
        for (int position : trainPositions) {
            trainInstance.setCursor(position);
            int rowIndex = trainInstance.getQualityFeature(userDimension);
            int columnIndex = trainInstance.getQualityFeature(itemDimension);
            // TODO 处理冲突
            trainTable.put(rowIndex, columnIndex, trainInstance.getQuantityMark());
        }
        SparseMatrix trainMatrix = SparseMatrix.valueOf(numberOfUsers, numberOfItems, trainTable);

        testPaginations = new int[numberOfUsers + 1];
        testPositions = new int[testMarker.getSize()];
        for (int index = 0; index < testMarker.getSize(); index++) {
            testPositions[index] = index;
        }
        DataMatcher testMatcher = DataMatcher.discreteOf(testMarker, userDimension);
        testMatcher.match(testPaginations, testPositions);
        DataSorter testSorter = DataSorter.featureOf(testMarker);
        testSorter.sort(testPaginations, testPositions);
        Table<Integer, Integer, Float> testTable = HashBasedTable.create();
        DataInstance testInstance = testMarker.getInstance(0);
        for (int position : testPositions) {
            testInstance.setCursor(position);
            int rowIndex = testInstance.getQualityFeature(userDimension);
            int columnIndex = testInstance.getQualityFeature(itemDimension);
            // TODO 处理冲突
            testTable.put(rowIndex, columnIndex, testInstance.getQuantityMark());
        }
        SparseMatrix testMatrix = SparseMatrix.valueOf(numberOfUsers, numberOfItems, testTable);

        int[] dataPaginations = new int[numberOfUsers + 1];
        int[] dataPositions = new int[dataMarker.getSize()];
        for (int index = 0; index < dataMarker.getSize(); index++) {
            dataPositions[index] = index;
        }
        DataMatcher dataMatcher = DataMatcher.discreteOf(dataMarker, userDimension);
        dataMatcher.match(dataPaginations, dataPositions);
        DataSorter dataSorter = DataSorter.featureOf(dataMarker);
        dataSorter.sort(dataPaginations, dataPositions);
        Table<Integer, Integer, Float> dataTable = HashBasedTable.create();
        DataInstance dataInstance = dataMarker.getInstance(0);
        for (int position : dataPositions) {
            dataInstance.setCursor(position);
            int rowIndex = dataInstance.getQualityFeature(userDimension);
            int columnIndex = dataInstance.getQualityFeature(itemDimension);
            // TODO 处理冲突
            dataTable.put(rowIndex, columnIndex, dataInstance.getQuantityMark());
        }
        SparseMatrix featureMatrix = SparseMatrix.valueOf(numberOfUsers, numberOfItems, dataTable);

        Recommender recommender = new MockRecommender(itemDimension, trainMatrix);
        Evaluator<T> evaluator = getEvaluator(featureMatrix);
        Int2FloatKeyValue sum = evaluate(evaluator, recommender);
        Assert.assertThat(sum.getValue() / sum.getKey(), CoreMatchers.equalTo(getMeasure()));
    }

    protected abstract T check(int userIndex);

    protected abstract List<Int2FloatKeyValue> recommend(Recommender recommender, int userIndex);

    private Int2FloatKeyValue evaluate(Evaluator<T> evaluator, Recommender recommender) {
        Int2FloatKeyValue sum = new Int2FloatKeyValue(0, 0F);
        for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
            // 测试映射
            if (testPaginations[userIndex + 1] - testPaginations[userIndex] == 0) {
                continue;
            }
            // 训练映射
            T checkCollection = check(userIndex);
            // 推荐列表
            List<Int2FloatKeyValue> recommendList = recommend(recommender, userIndex);
            // 测量列表
            Int2FloatKeyValue measure = evaluator.evaluate(checkCollection, recommendList);
            sum.setKey(sum.getKey() + measure.getKey());
            sum.setValue(sum.getValue() + measure.getValue());
        }
        return sum;
    }

}
