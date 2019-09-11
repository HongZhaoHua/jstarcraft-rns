package com.jstarcraft.rns.model.context;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.model.context.ranking.RankGeoFMModelTestCase;
import com.jstarcraft.rns.model.context.ranking.SBPRModelTestCase;
import com.jstarcraft.rns.model.context.rating.RSTEModelTestCase;
import com.jstarcraft.rns.model.context.rating.SoRecModelTestCase;
import com.jstarcraft.rns.model.context.rating.SoRegModelTestCase;
import com.jstarcraft.rns.model.context.rating.SocialMFModelTestCase;
import com.jstarcraft.rns.model.context.rating.TimeSVDModelTestCase;
import com.jstarcraft.rns.model.context.rating.TrustMFModelTestCase;
import com.jstarcraft.rns.model.context.rating.TrustSVDModelTestCase;

@RunWith(Suite.class)
@SuiteClasses({
        // 推荐器测试集

        // recommender.context.ranking
        RankGeoFMModelTestCase.class,

        SBPRModelTestCase.class,

        // recommender.context.rating
        RSTEModelTestCase.class,

        SocialMFModelTestCase.class,

        SoRecModelTestCase.class,

        SoRegModelTestCase.class,

        TimeSVDModelTestCase.class,

        TrustMFModelTestCase.class,

        TrustSVDModelTestCase.class })
public class ContextTestSuite {

}
