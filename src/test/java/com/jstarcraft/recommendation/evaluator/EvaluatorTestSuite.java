package com.jstarcraft.recommendation.evaluator;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.recommendation.evaluator.rank.AUCEvaluatorTestCase;
import com.jstarcraft.recommendation.evaluator.rank.DiversityEvaluatorTestCase;
import com.jstarcraft.recommendation.evaluator.rank.MAPEvaluatorTestCase;
import com.jstarcraft.recommendation.evaluator.rank.MRREvaluatorTestCase;
import com.jstarcraft.recommendation.evaluator.rank.NDCGEvaluatorTestCase;
import com.jstarcraft.recommendation.evaluator.rank.PrecisionEvaluatorTestCase;
import com.jstarcraft.recommendation.evaluator.rank.RecallEvaluatorTestCase;
import com.jstarcraft.recommendation.evaluator.rate.MAEEvaluatorTestCase;
import com.jstarcraft.recommendation.evaluator.rate.MPEEvaluatorTestCase;
import com.jstarcraft.recommendation.evaluator.rate.MSEEvaluatorTestCase;

@RunWith(Suite.class)
@SuiteClasses({
		// 评估器测试集
		AUCEvaluatorTestCase.class,

		MAPEvaluatorTestCase.class,

		DiversityEvaluatorTestCase.class,

		NDCGEvaluatorTestCase.class,

		PrecisionEvaluatorTestCase.class,

		RecallEvaluatorTestCase.class,

		MRREvaluatorTestCase.class,

		MAEEvaluatorTestCase.class,

		MPEEvaluatorTestCase.class,

		MSEEvaluatorTestCase.class })
public class EvaluatorTestSuite {

}
