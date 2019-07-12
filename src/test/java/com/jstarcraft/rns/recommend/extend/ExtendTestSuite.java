package com.jstarcraft.rns.recommend.extend;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.recommend.extend.ranking.AssociationRuleTestCase;
import com.jstarcraft.rns.recommend.extend.ranking.PRankDTestCase;
import com.jstarcraft.rns.recommend.extend.rating.PersonalityDiagnosisTestCase;
import com.jstarcraft.rns.recommend.extend.rating.SlopeOneTestCase;

@RunWith(Suite.class)
@SuiteClasses({
		// 推荐器测试集
		AssociationRuleTestCase.class,

		PersonalityDiagnosisTestCase.class,

		PRankDTestCase.class,

		SlopeOneTestCase.class })
public class ExtendTestSuite {

}
