package com.jstarcraft.rns.recommend.collaborative.rating;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
		// 推荐器测试集
		// recommender.cf.rating
		AspectModelRatingRecommenderTestCase.class,

		ASVDPlusPlusRecommenderTestCase.class,

		BiasedMFRecommenderTestCase.class,

		BHFreeRatingRecommenderTestCase.class,

		BPMFRecommenderTestCase.class,

		BUCMRatingRecommenderTestCase.class,

		FFMRecommenderTestCase.class,

		FMALSRecommenderTestCase.class,

		FMSGDRecommenderTestCase.class,

		GPLSARecommenderTestCase.class,

		IRRGRecommenderTestCase.class,

		ItemKNNRatingRecommenderTestCase.class,

		LDCCRecommenderTestCase.class,

		LLORMARecommenderTestCase.class,

		MFALSRecommenderTestCase.class,

		NMFRecommenderTestCase.class,

		PMFRecommenderTestCase.class,

		RBMRecommenderTestCase.class,

		RFRecRecommenderTestCase.class,

		SVDPlusPlusRecommenderTestCase.class,

		URPRecommenderTestCase.class,

		UserKNNRatingRecommenderTestCase.class, })
public class CollaborativeRatingTestSuite {

}
