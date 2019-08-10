package com.jstarcraft.rns.model.extend;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.model.extend.ranking.AssociationRuleRecommenderTestCase;
import com.jstarcraft.rns.model.extend.ranking.PRankDRecommenderTestCase;
import com.jstarcraft.rns.model.extend.rating.PersonalityDiagnosisRecommenderTestCase;
import com.jstarcraft.rns.model.extend.rating.SlopeOneRecommenderTestCase;

@RunWith(Suite.class)
@SuiteClasses({
		// 推荐器测试集
		AssociationRuleRecommenderTestCase.class,

		PersonalityDiagnosisRecommenderTestCase.class,

		PRankDRecommenderTestCase.class,

		SlopeOneRecommenderTestCase.class })
public class ExtendTestSuite {

}
