package com.jstarcraft.rns.recommend;

import java.util.LinkedHashMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.environment.EnvironmentContext;
import com.jstarcraft.ai.math.structure.matrix.HashMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.core.resource.annotation.ResourceConfiguration;
import com.jstarcraft.core.resource.annotation.ResourceId;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.configure.Configurator;
import com.jstarcraft.rns.data.processor.DataMatcher;
import com.jstarcraft.rns.data.processor.DataSorter;

import it.unimi.dsi.fastutil.ints.Int2FloatRBTreeMap;

/**
 * 抽象推荐器
 * 
 * @author Birdy
 *
 */
@ResourceConfiguration
public abstract class AbstractRecommender implements Recommender {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @ResourceId
    protected String id;

    // 参数部分
    /** 用户字段, 物品字段, 分数字段 */
    protected String userField, itemField;

    /** 用户维度, 物品维度 */
    protected int userDimension, itemDimension;

    /** 用户数量, 物品数量 */
    protected int userSize, itemSize;

    /** 最低分数, 最高分数, 平均分数 */
    protected float minimumOfScore, maximumOfScore, meanOfScore;

    /** 分数索引 (TODO 考虑取消或迁移.本质为连续特征离散化) */
    protected LinkedHashMap<Float, Integer> scoreIndexes;

    /** 行为数量(TODO 此字段可能迁移到其它类.为避免重复行为,一般使用matrix或者tensor的元素数量) */
    protected int numberOfActions;

    /** 训练矩阵(TODO 准备改名为actionMatrix或者scoreMatrix) */
    protected SparseMatrix scoreMatrix;

    protected int[] dataPaginations;
    protected int[] dataPositions;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        userField = configuration.getString("data.model.fields.user", "user");
        itemField = configuration.getString("data.model.fields.item", "item");

        userDimension = model.getQualityInner(userField);
        itemDimension = model.getQualityInner(itemField);
        userSize = space.getQualityAttribute(userField).getSize();
        itemSize = space.getQualityAttribute(itemField).getSize();

        dataPaginations = new int[userSize + 1];
        dataPositions = new int[model.getSize()];
        for (int index = 0; index < model.getSize(); index++) {
            dataPositions[index] = index;
        }
        DataMatcher matcher = DataMatcher.discreteOf(model, userDimension);
        matcher.match(dataPaginations, dataPositions);
        DataSorter sorter = DataSorter.featureOf(model);
        sorter.sort(dataPaginations, dataPositions);
        HashMatrix dataTable = new HashMatrix(true, userSize, itemSize, new Int2FloatRBTreeMap());
        DataInstance instance = model.getInstance(0);
        for (int position : dataPositions) {
            instance.setCursor(position);
            int rowIndex = instance.getQualityFeature(userDimension);
            int columnIndex = instance.getQualityFeature(itemDimension);
            dataTable.setValue(rowIndex, columnIndex, instance.getQuantityMark());
        }
        scoreMatrix = SparseMatrix.valueOf(userSize, itemSize, dataTable);
        numberOfActions = scoreMatrix.getElementSize();

        // TODO 此处会与scoreIndexes一起重构,本质为连续特征离散化.
        TreeSet<Float> values = new TreeSet<>();
        for (MatrixScalar term : scoreMatrix) {
            values.add(term.getValue());
        }
        values.remove(0F);
        scoreIndexes = new LinkedHashMap<>();
        Integer index = 0;
        for (Float value : values) {
            scoreIndexes.put(value, index++);
        }
        KeyValue<Float, Float> attribute = scoreMatrix.getBoundary(false);
        minimumOfScore = attribute.getKey();
        maximumOfScore = attribute.getValue();
        meanOfScore = scoreMatrix.getSum(false);
        meanOfScore /= numberOfActions;
    }

    protected abstract void doPractice();

    protected void constructEnvironment() {
    }

    protected void destructEnvironment() {
    }

    @Override
    public final void practice() {
        EnvironmentContext context = EnvironmentContext.getContext();
        context.doAlgorithmByEvery(this::constructEnvironment);
        doPractice();
        context.doAlgorithmByEvery(this::destructEnvironment);
    }

}
