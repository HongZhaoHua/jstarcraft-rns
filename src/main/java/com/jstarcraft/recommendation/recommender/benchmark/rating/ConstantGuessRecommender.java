package com.jstarcraft.recommendation.recommender.benchmark.rating;

import com.jstarcraft.ai.modem.ModemDefinition;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.DenseModule;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;
import com.jstarcraft.recommendation.recommender.AbstractRecommender;

/**
 * 
 * Constant Guess推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
@ModemDefinition(value = { "constant" })
public class ConstantGuessRecommender extends AbstractRecommender {

	private float constant;

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, DenseModule model, DataSpace space) {
		super.prepare(configuration, marker, model, space);
		// 默认使用最高最低分的平均值
		constant = (minimumOfScore + maximumOfScore) / 2F;
		// TODO 支持配置分数
		constant = configuration.getFloat("recommend.constant-guess.score", constant);
	}

	@Override
	protected void doPractice() {
	}

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		return constant;
	}

}
