package com.jstarcraft.rns.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.processor.DataSorter;
import com.jstarcraft.ai.data.processor.DataSplitter;
import com.jstarcraft.ai.environment.EnvironmentContext;
import com.jstarcraft.ai.math.structure.matrix.HashMatrix;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.data.processor.AllFeatureDataSorter;
import com.jstarcraft.rns.data.processor.QualityFeatureDataSplitter;

import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;

/**
 * 抽象推荐器
 * 
 * @author Birdy
 *
 */
public abstract class AbstractModel implements Model {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected String id;

    // 参数部分
    /** 用户字段, 物品字段, 分数字段 */
    protected String userField, itemField;

    /** 用户维度, 物品维度 */
    protected int userDimension, itemDimension;

    /** 用户数量, 物品数量 */
    protected int userSize, itemSize;

    /** 最低分数, 最高分数, 平均分数 */
    protected float minimumScore, maximumScore, meanScore;

    /** 行为数量(TODO 此字段可能迁移到其它类.为避免重复行为,一般使用matrix或者tensor的元素数量) */
    protected int actionSize;

    /** 训练矩阵(TODO 准备改名为actionMatrix或者scoreMatrix) */
    protected SparseMatrix scoreMatrix;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        userField = configuration.getString("data.model.fields.user", "user");
        itemField = configuration.getString("data.model.fields.item", "item");

        userDimension = model.getQualityInner(userField);
        itemDimension = model.getQualityInner(itemField);
        userSize = space.getQualityAttribute(userField).getSize();
        itemSize = space.getQualityAttribute(itemField).getSize();

        DataSplitter splitter = new QualityFeatureDataSplitter(userDimension);
        DataModule[] models = splitter.split(model, userSize);
        DataSorter sorter = new AllFeatureDataSorter();
        for (int index = 0; index < userSize; index++) {
            models[index] = sorter.sort(models[index]);
        }

        HashMatrix dataTable = new HashMatrix(true, userSize, itemSize, new Long2FloatRBTreeMap());
        for (DataInstance instance : model) {
            int rowIndex = instance.getQualityFeature(userDimension);
            int columnIndex = instance.getQualityFeature(itemDimension);
            dataTable.setValue(rowIndex, columnIndex, instance.getQuantityMark());
        }
        scoreMatrix = SparseMatrix.valueOf(userSize, itemSize, dataTable);
        actionSize = scoreMatrix.getElementSize();
        KeyValue<Float, Float> attribute = scoreMatrix.getBoundary(false);
        minimumScore = attribute.getKey();
        maximumScore = attribute.getValue();
        meanScore = scoreMatrix.getSum(false);
        meanScore /= actionSize;
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
