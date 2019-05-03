package com.jstarcraft.recommendation.recommender;

import java.util.LinkedHashMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jstarcraft.ai.data.attribute.QuantityAttribute;
import com.jstarcraft.ai.environment.EnvironmentContext;
import com.jstarcraft.ai.math.structure.matrix.HashMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.DenseModule;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;
import com.jstarcraft.recommendation.data.processor.DataMatcher;
import com.jstarcraft.recommendation.data.processor.DataSorter;

import it.unimi.dsi.fastutil.ints.Int2FloatRBTreeMap;

/**
 * 抽象推荐器
 * 
 * @author Birdy
 *
 */
public abstract class AbstractRecommender implements Recommender {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	/** 用户字段, 物品字段, 分数字段 */
	protected String userField, itemField, scoreField;

	/** 用户维度, 物品维度 */
	protected int userDimension, itemDimension;

	/** 用户数量, 物品数量 */
	protected int numberOfUsers, numberOfItems;

	/** 行为数量(TODO 此字段可能迁移到其它类.为避免重复行为,一般使用matrix或者tensor的元素数量) */
	protected int numberOfActions;

	/** 训练矩阵(TODO 准备改名为actionMatrix或者scoreMatrix) */
	protected SparseMatrix trainMatrix;

	/** 测试矩阵(TODO 准备取消) */
	protected SparseMatrix testMatrix;

	/** 最低分数, 最高分数, 平均分数 */
	protected float minimumOfScore, maximumOfScore, meanOfScore;

	/** 分数索引 (TODO 考虑取消或迁移.本质为连续特征离散化) */
	protected LinkedHashMap<Float, Integer> scoreIndexes;

	protected int[] dataPaginations;
	protected int[] dataPositions;

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, DenseModule model, DataSpace space) {
		userField = configuration.getString("data.model.fields.user", "user");
		itemField = configuration.getString("data.model.fields.item", "item");
		scoreField = configuration.getString("data.model.fields.score", "score");

		userDimension = model.getQualityInner(userField);
		itemDimension = model.getQualityInner(itemField);
		numberOfUsers = model.getQualityAttribute(userDimension).getSize();
		numberOfItems = model.getQualityAttribute(itemDimension).getSize();

		dataPaginations = new int[numberOfUsers + 1];
		dataPositions = new int[marker.getSize()];
		for (int index = 0; index < marker.getSize(); index++) {
			dataPositions[index] = index;
		}
		DataMatcher matcher = DataMatcher.discreteOf(marker, userDimension);
		matcher.match(dataPaginations, dataPositions);
		DataSorter sorter = DataSorter.featureOf(marker);
		sorter.sort(dataPaginations, dataPositions);
		HashMatrix dataTable = HashMatrix.valueOf(true, numberOfUsers, numberOfItems, new Int2FloatRBTreeMap());
		for (int position : dataPositions) {
			int rowIndex = marker.getQualityFeature(userDimension, position);
			int columnIndex = marker.getQualityFeature(itemDimension, position);
			dataTable.setValue(rowIndex, columnIndex, marker.getMark(position));
		}
		trainMatrix = SparseMatrix.valueOf(numberOfUsers, numberOfItems, dataTable);
		numberOfActions = trainMatrix.getElementSize();

		// TODO 此处会与scoreIndexes一起重构,本质为连续特征离散化.
		TreeSet<Float> values = new TreeSet<>();
		for (MatrixScalar term : trainMatrix) {
			values.add(term.getValue());
		}
		values.remove(0F);
		scoreIndexes = new LinkedHashMap<>();
		Integer index = 0;
		for (Float value : values) {
			scoreIndexes.put(value, index++);
		}
		QuantityAttribute attribute = model.getQuantityAttribute(model.getQuantityInner(scoreField));
		minimumOfScore = (Float) attribute.getMinimum();
		maximumOfScore = (Float) attribute.getMaximum();
		meanOfScore = trainMatrix.getSum(false);
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
