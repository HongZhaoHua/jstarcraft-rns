package com.jstarcraft.ai.evaluate;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.ai.evaluate.ranking.AUCEvaluatorTestCase;
import com.jstarcraft.ai.evaluate.ranking.DiversityEvaluatorTestCase;
import com.jstarcraft.ai.evaluate.ranking.MAPEvaluatorTestCase;
import com.jstarcraft.ai.evaluate.ranking.MRREvaluatorTestCase;
import com.jstarcraft.ai.evaluate.ranking.NDCGEvaluatorTestCase;
import com.jstarcraft.ai.evaluate.ranking.PrecisionEvaluatorTestCase;
import com.jstarcraft.ai.evaluate.ranking.RecallEvaluatorTestCase;
import com.jstarcraft.ai.evaluate.rating.MAEEvaluatorTestCase;
import com.jstarcraft.ai.evaluate.rating.MPEEvaluatorTestCase;
import com.jstarcraft.ai.evaluate.rating.MSEEvaluatorTestCase;

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
