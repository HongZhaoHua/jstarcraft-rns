package com.jstarcraft.rns.recommend;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.rns.configure.Configurator;

/**
 * 推荐器
 * 
 * <pre>
 * 注意区分每个阶段的职责:
 * 准备阶段关注数据,负责根据算法转换数据;
 * 训练阶段关注参数,负责根据参数获得模型;
 * 预测阶段关注模型,负责根据模型预测得分;
 * </pre>
 * 
 * @author Birdy
 *
 */
public interface Recommender {

	/**
	 * 准备
	 * 
	 * @param configurator
	 */
	void prepare(Configurator configurator, DataModule module, DataSpace space);
	// void prepare(Configuration configuration, SparseTensor trainTensor,
	// SparseTensor testTensor, DataSpace storage);

	/**
	 * 训练
	 * 
	 * @param trainTensor
	 * @param testTensor
	 * @param contextModels
	 */
	void practice();

	/**
	 * 预测
	 * 
	 * @param userIndex
	 * @param itemIndex
	 * @param featureIndexes
	 * @return
	 */
	void predict(DataInstance instance);
	// double predict(int userIndex, int itemIndex, int... featureIndexes);

}
