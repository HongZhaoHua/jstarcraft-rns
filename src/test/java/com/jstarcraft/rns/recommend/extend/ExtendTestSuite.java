package com.jstarcraft.rns.recommend.extend;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.recommend.extend.ranking.AssociationRuleRecommenderTestCase;
import com.jstarcraft.rns.recommend.extend.ranking.PRankDRecommenderTestCase;
import com.jstarcraft.rns.recommend.extend.rating.PersonalityDiagnosisRecommenderTestCase;
import com.jstarcraft.rns.recommend.extend.rating.SlopeOneRecommenderTestCase;

@RunWith(Suite.class)
@SuiteClasses({
		// 推荐器测试集
		AssociationRuleRecommenderTestCase.class,

		PersonalityDiagnosisRecommenderTestCase.class,

		PRankDRecommenderTestCase.class,

		SlopeOneRecommenderTestCase.class })
public class ExtendTestSuite {

}
