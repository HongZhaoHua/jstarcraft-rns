package com.jstarcraft.recommendation.recommender.context;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.recommendation.recommender.context.ranking.RankGeoFMTestCase;
import com.jstarcraft.recommendation.recommender.context.ranking.SBPRTestCase;
import com.jstarcraft.recommendation.recommender.context.rating.RSTETestCase;
import com.jstarcraft.recommendation.recommender.context.rating.SoRecTestCase;
import com.jstarcraft.recommendation.recommender.context.rating.SoRegTestCase;
import com.jstarcraft.recommendation.recommender.context.rating.SocialMFTestCase;
import com.jstarcraft.recommendation.recommender.context.rating.TimeSVDTestCase;
import com.jstarcraft.recommendation.recommender.context.rating.TrustMFTestCase;
import com.jstarcraft.recommendation.recommender.context.rating.TrustSVDTestCase;

@RunWith(Suite.class)
@SuiteClasses({
		// 推荐器测试集

		// recommender.context.ranking
		RankGeoFMTestCase.class,

		SBPRTestCase.class,

		// recommender.context.rating
		RSTETestCase.class,

		SocialMFTestCase.class,

		SoRecTestCase.class,

		SoRegTestCase.class,

		TimeSVDTestCase.class,

		TrustMFTestCase.class,

		TrustSVDTestCase.class })
public class ContextTestSuite {

}
