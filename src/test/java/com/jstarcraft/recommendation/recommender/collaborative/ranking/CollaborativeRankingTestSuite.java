package com.jstarcraft.recommendation.recommender.collaborative.ranking;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
		// 推荐器测试集
		// recommender.cf.ranking
		AOBPRTestCase.class,

		AspectModelRankingTestCase.class,

		BHFreeRankingTestCase.class,

		BPRTestCase.class,

		BUCMRankingTestCase.class,

		CDAETestCase.class,

		CLIMFTestCase.class,

		DeepFMTestCase.class,

		EALSTestCase.class,

		FISMAUCTestCase.class,

		FISMRMSETestCase.class,

		GBPRTestCase.class,

		HMMTestCase.class,

		ItemBigramTestCase.class,

		ItemKNNRankingTestCase.class,

		LDATestCase.class,

		LambdaFMTestCase.class,

		ListwiseMFTestCase.class,

		PLSATestCase.class,

		RankALSTestCase.class,

		RankSGDTestCase.class,

		SLIMTestCase.class,

		UserKNNRankingTestCase.class,

		WBPRTestCase.class,

		WRMFTestCase.class, })
public class CollaborativeRankingTestSuite {

}
