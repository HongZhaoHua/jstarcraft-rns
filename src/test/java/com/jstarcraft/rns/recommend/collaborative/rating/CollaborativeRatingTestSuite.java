package com.jstarcraft.rns.recommend.collaborative.rating;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
		// 推荐器测试集
		// recommender.cf.rating
		AspectModelRatingTestCase.class,

		ASVDPlusPlusTestCase.class,

		BiasedMFTestCase.class,

		BHFreeRatingTestCase.class,

		BPMFTestCase.class,

		BUCMRatingTestCase.class,

		FFMTestCase.class,

		FMALSTestCase.class,

		FMSGDTestCase.class,

		GPLSATestCase.class,

		IRRGTestCase.class,

		ItemKNNRatingTestCase.class,

		LDCCTestCase.class,

		LLORMATestCase.class,

		MFALSTestCase.class,

		NMFTestCase.class,

		PMFTestCase.class,

		RBMTestCase.class,

		RFRecTestCase.class,

		SVDPlusPlusTestCase.class,

		URPTestCase.class,

		UserKNNRatingTestCase.class, })
public class CollaborativeRatingTestSuite {

}
