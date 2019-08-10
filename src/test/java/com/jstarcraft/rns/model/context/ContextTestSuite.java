package com.jstarcraft.rns.model.context;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.model.context.ranking.RankGeoFMRecommenderTestCase;
import com.jstarcraft.rns.model.context.ranking.SBPRRecommenderTestCase;
import com.jstarcraft.rns.model.context.rating.RSTERecommenderTestCase;
import com.jstarcraft.rns.model.context.rating.SoRecRecommenderTestCase;
import com.jstarcraft.rns.model.context.rating.SoRegRecommenderTestCase;
import com.jstarcraft.rns.model.context.rating.SocialMFRecommenderTestCase;
import com.jstarcraft.rns.model.context.rating.TimeSVDRecommenderTestCase;
import com.jstarcraft.rns.model.context.rating.TrustMFRecommenderTestCase;
import com.jstarcraft.rns.model.context.rating.TrustSVDRecommenderTestCase;

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
