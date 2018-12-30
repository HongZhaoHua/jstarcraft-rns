package com.jstarcraft.recommendation.data.splitter;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
		// 分割器测试集
		GivenDataSplitterTestCase.class,

		GivenInstanceSplitterTestCase.class,

		GivenNumberSplitterTestCase.class,

		KFoldCrossValidationSplitterTestCase.class,

		LeaveOneCrossValidationSplitterTestCase.class,

		RandomSplitterTestCase.class,

		RatioSplitterTestCase.class })
public class SplitterTestSuite {

}
