package com.jstarcraft.recommendation.recommender.extend;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.recommendation.recommender.extend.ranking.AssociationRuleTestCase;
import com.jstarcraft.recommendation.recommender.extend.ranking.PRankDTestCase;
import com.jstarcraft.recommendation.recommender.extend.rating.PersonalityDiagnosisTestCase;
import com.jstarcraft.recommendation.recommender.extend.rating.SlopeOneTestCase;

@RunWith(Suite.class)
@SuiteClasses({
		// 推荐器测试集
		AssociationRuleTestCase.class,

		PersonalityDiagnosisTestCase.class,

		PRankDTestCase.class,

		SlopeOneTestCase.class })
public class ExtendTestSuite {

}
