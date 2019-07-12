package com.jstarcraft.rns.recommend.context;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.recommend.context.ranking.RankGeoFMTestCase;
import com.jstarcraft.rns.recommend.context.ranking.SBPRTestCase;
import com.jstarcraft.rns.recommend.context.rating.RSTETestCase;
import com.jstarcraft.rns.recommend.context.rating.SoRecTestCase;
import com.jstarcraft.rns.recommend.context.rating.SoRegTestCase;
import com.jstarcraft.rns.recommend.context.rating.SocialMFTestCase;
import com.jstarcraft.rns.recommend.context.rating.TimeSVDTestCase;
import com.jstarcraft.rns.recommend.context.rating.TrustMFTestCase;
import com.jstarcraft.rns.recommend.context.rating.TrustSVDTestCase;

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
