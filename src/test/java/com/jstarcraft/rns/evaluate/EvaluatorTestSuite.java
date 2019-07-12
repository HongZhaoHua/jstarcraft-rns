package com.jstarcraft.rns.evaluate;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.evaluate.rank.AUCEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.rank.DiversityEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.rank.MAPEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.rank.MRREvaluatorTestCase;
import com.jstarcraft.rns.evaluate.rank.NDCGEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.rank.PrecisionEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.rank.RecallEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.rate.MAEEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.rate.MPEEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.rate.MSEEvaluatorTestCase;

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
