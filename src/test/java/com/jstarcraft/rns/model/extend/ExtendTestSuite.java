package com.jstarcraft.rns.model.extend;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.model.extend.ranking.AssociationRuleModelTestCase;
import com.jstarcraft.rns.model.extend.ranking.PRankDModelTestCase;
import com.jstarcraft.rns.model.extend.rating.PersonalityDiagnosisModelTestCase;
import com.jstarcraft.rns.model.extend.rating.SlopeOneModelTestCase;

@RunWith(Suite.class)
@SuiteClasses({
        // 推荐器测试集
        AssociationRuleModelTestCase.class,

        PersonalityDiagnosisModelTestCase.class,

        PRankDModelTestCase.class,

        SlopeOneModelTestCase.class })
public class ExtendTestSuite {

}
