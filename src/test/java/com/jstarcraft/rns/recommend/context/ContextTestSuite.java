package com.jstarcraft.rns.recommend.context;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.recommend.context.ranking.RankGeoFMRecommenderTestCase;
import com.jstarcraft.rns.recommend.context.ranking.SBPRRecommenderTestCase;
import com.jstarcraft.rns.recommend.context.rating.RSTERecommenderTestCase;
import com.jstarcraft.rns.recommend.context.rating.SoRecRecommenderTestCase;
import com.jstarcraft.rns.recommend.context.rating.SoRegRecommenderTestCase;
import com.jstarcraft.rns.recommend.context.rating.SocialMFRecommenderTestCase;
import com.jstarcraft.rns.recommend.context.rating.TimeSVDRecommenderTestCase;
import com.jstarcraft.rns.recommend.context.rating.TrustMFRecommenderTestCase;
import com.jstarcraft.rns.recommend.context.rating.TrustSVDRecommenderTestCase;

@RunWith(Suite.class)
@SuiteClasses({
		// 推荐器测试集

		// recommender.context.ranking
		RankGeoFMRecommenderTestCase.class,

		SBPRRecommenderTestCase.class,

		// recommender.context.rating
		RSTERecommenderTestCase.class,

		SocialMFRecommenderTestCase.class,

		SoRecRecommenderTestCase.class,

		SoRegRecommenderTestCase.class,

		TimeSVDRecommenderTestCase.class,

		TrustMFRecommenderTestCase.class,

		TrustSVDRecommenderTestCase.class })
public class ContextTestSuite {

}
