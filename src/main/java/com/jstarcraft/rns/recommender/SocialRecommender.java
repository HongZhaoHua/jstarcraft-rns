package com.jstarcraft.rns.recommender;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.rns.configurator.Configuration;

/**
 * 社交推荐器
 * 
 * <pre>
 * 注意:基缘,是指构成人际关系的最基本的因素,包括血缘,地缘,业缘,趣缘.
 * 实际业务使用过程中要注意人与人之间社区关系(趣缘)与社会关系(血缘,地缘,业缘)的区分.
 * </pre>
 * 
 * @author Birdy
 *
 */
public abstract class SocialRecommender extends MatrixFactorizationRecommender {

	protected String trusterField, trusteeField, coefficientField;

	protected int trusterDimension, trusteeDimension, coefficientDimension;
	/**
	 * socialMatrix: social rate matrix, indicating a user is connecting to a number
	 * of other users
	 */
	// TODO 此处是不是应该使用context.getSimilarity().getSimilarityMatrix();代替?
	protected SparseMatrix socialMatrix;

	/**
	 * social regularization
	 */
	protected float socialRegularization;

	@Override
	public void prepare(Configuration configuration, DataModule model, DataSpace space) {
		super.prepare(configuration, model, space);

		socialRegularization = configuration.getFloat("rec.social.regularization", 0.01f);
		// social path for the socialMatrix
		// TODO 此处是不是应该使用context.getSimilarity().getSimilarityMatrix();代替?
		DataModule socialModel = space.getModule("social");
		// TODO 此处需要重构,trusterDimension与trusteeDimension要配置
		trusterField = configuration.getString("data.model.fields.truster");
		trusteeField = configuration.getString("data.model.fields.trustee");
		coefficientField = configuration.getString("data.model.fields.coefficient");
		trusterDimension = 0;
		trusteeDimension = 1;
		coefficientDimension = socialModel.getQuantityInner(coefficientField);
		Table<Integer, Integer, Float> socialTabel = HashBasedTable.create();
		for (DataInstance instance : socialModel) {
			socialTabel.put(instance.getQualityFeature(trusterDimension), instance.getQualityFeature(trusteeDimension), instance.getQuantityFeature(coefficientDimension));
		}
		socialMatrix = SparseMatrix.valueOf(numberOfUsers, numberOfUsers, socialTabel);
	}

	/**
	 * 逆态化
	 * 
	 * <pre>
	 * 把数值从(0,1)转换为(minimumOfScore,maximumOfScore)
	 * </pre>
	 * 
	 * @param value
	 * @return
	 */
	protected float denormalize(float value) {
		return minimumOfScore + value * (maximumOfScore - minimumOfScore);
	}

	/**
	 * 正态化
	 * 
	 * <pre>
	 * 把数值从(minimumOfScore,maximumOfScore)转换为(0,1)
	 * </pre>
	 * 
	 * @param value
	 * @return
	 */
	protected float normalize(float value) {
		return (value - minimumOfScore) / (maximumOfScore - minimumOfScore);
	}

}
