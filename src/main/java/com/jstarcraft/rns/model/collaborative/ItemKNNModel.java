package com.jstarcraft.rns.model.collaborative;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeSet;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.algorithm.similarity.Similarity;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.matrix.SymmetryMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.core.common.reflection.ReflectionUtility;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.rns.model.AbstractModel;

/**
 * 
 * Item KNN推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public abstract class ItemKNNModel extends AbstractModel {

	/** 邻居数量 */
	private int neighborSize;

	protected SymmetryMatrix similarityMatrix;

	protected DenseVector itemMeans;

	/**
	 * item's nearest neighbors for kNN > 0
	 */
	protected int[][] itemNeighbors;

	protected SparseVector[] userVectors;

	protected SparseVector[] itemVectors;

	private Comparator<Entry<Integer, Double>> comparator = new Comparator<Entry<Integer, Double>>() {
		public int compare(Entry<Integer, Double> left, Entry<Integer, Double> right) {
			int value = -(left.getValue().compareTo(right.getValue()));
			if (value == 0) {
				value = left.getKey().compareTo(right.getKey());
			}
			return value;
		}
	};

	@Override
	public void prepare(Configurator configuration, DataModule model, DataSpace space) {
		super.prepare(configuration, model, space);
		neighborSize = configuration.getInteger("recommender.neighbors.knn.number", 50);
		// TODO 修改为配置枚举
		try {
			Class<Similarity> similarityClass = (Class<Similarity>) Class.forName(configuration.getString("recommender.similarity.class"));
			Similarity similarity = ReflectionUtility.getInstance(similarityClass);
			similarityMatrix = similarity.makeSimilarityMatrix(scoreMatrix, true, configuration.getFloat("recommender.similarity.shrinkage", 0F));
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		}
		itemMeans = DenseVector.valueOf(itemSize);

		// TODO 设置容量
		itemNeighbors = new int[itemSize][];
		HashMap<Integer, TreeSet<Entry<Integer, Double>>> itemNNs = new HashMap<>();
		for (MatrixScalar term : similarityMatrix) {
			int row = term.getRow();
			int column = term.getColumn();
			double value = term.getValue();
			if (row == column) {
				continue;
			}
			// 忽略相似度为0的物品
			if (value == 0D) {
				continue;
			}
			TreeSet<Entry<Integer, Double>> neighbors = itemNNs.get(row);
			if (neighbors == null) {
				neighbors = new TreeSet<>(comparator);
				itemNNs.put(row, neighbors);
			}
			neighbors.add(new SimpleImmutableEntry<>(column, value));
			neighbors = itemNNs.get(column);
			if (neighbors == null) {
				neighbors = new TreeSet<>(comparator);
				itemNNs.put(column, neighbors);
			}
			neighbors.add(new SimpleImmutableEntry<>(row, value));
		}

		// 构建用户邻居映射
		for (Entry<Integer, TreeSet<Entry<Integer, Double>>> term : itemNNs.entrySet()) {
			TreeSet<Entry<Integer, Double>> neighbors = term.getValue();
			int[] value = new int[neighbors.size() < neighborSize ? neighbors.size() : neighborSize];
			int index = 0;
			for (Entry<Integer, Double> neighbor : neighbors) {
				value[index++] = neighbor.getKey();
				if (index >= neighborSize) {
					break;
				}
			}
			Arrays.sort(value);
			itemNeighbors[term.getKey()] = value;
		}

		userVectors = new SparseVector[userSize];
		for (int userIndex = 0; userIndex < userSize; userIndex++) {
			userVectors[userIndex] = scoreMatrix.getRowVector(userIndex);
		}

		itemVectors = new SparseVector[itemSize];
		for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
			itemVectors[itemIndex] = scoreMatrix.getColumnVector(itemIndex);
		}
	}

	@Override
	protected void doPractice() {
		meanScore = scoreMatrix.getSum(false) / scoreMatrix.getElementSize();
		for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
			SparseVector itemVector = scoreMatrix.getColumnVector(itemIndex);
			itemMeans.setValue(itemIndex, itemVector.getElementSize() > 0 ? itemVector.getSum(false) / itemVector.getElementSize() : meanScore);
		}
	}

}
