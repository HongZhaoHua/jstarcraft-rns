package com.jstarcraft.rns.evaluator;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.evaluator.rank.AUCEvaluatorTestCase;
import com.jstarcraft.rns.evaluator.rank.DiversityEvaluatorTestCase;
import com.jstarcraft.rns.evaluator.rank.MAPEvaluatorTestCase;
import com.jstarcraft.rns.evaluator.rank.MRREvaluatorTestCase;
import com.jstarcraft.rns.evaluator.rank.NDCGEvaluatorTestCase;
import com.jstarcraft.rns.evaluator.rank.PrecisionEvaluatorTestCase;
import com.jstarcraft.rns.evaluator.rank.RecallEvaluatorTestCase;
import com.jstarcraft.rns.evaluator.rate.MAEEvaluatorTestCase;
import com.jstarcraft.rns.evaluator.rate.MPEEvaluatorTestCase;
import com.jstarcraft.rns.evaluator.rate.MSEEvaluatorTestCase;

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
