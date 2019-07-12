package com.jstarcraft.rns.evaluate;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.evaluate.ranking.AUCEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.ranking.DiversityEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.ranking.MAPEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.ranking.MRREvaluatorTestCase;
import com.jstarcraft.rns.evaluate.ranking.NDCGEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.ranking.PrecisionEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.ranking.RecallEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.rating.MAEEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.rating.MPEEvaluatorTestCase;
import com.jstarcraft.rns.evaluate.rating.MSEEvaluatorTestCase;

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
