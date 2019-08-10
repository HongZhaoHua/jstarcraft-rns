package com.jstarcraft.rns.model.collaborative.ranking;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        // 推荐器测试集
        // recommender.cf.ranking
        AoBPRModelTestCase.class,

        AspectModelRankingModelTestCase.class,

        BHFreeRankingModelTestCase.class,

        BPRModelTestCase.class,

        BUCMRankingModelTestCase.class,

        CDAEModelTestCase.class,

        CLiMFModelTestCase.class,

//        DeepCrossTestCase.class,

        DeepFMModelTestCase.class,

        EALSModelTestCase.class,

        FISMAUCModelTestCase.class,

        FISMRMSEModelTestCase.class,

        GBPRModelTestCase.class,

        HMMModelTestCase.class,

        ItemBigramModelTestCase.class,

        ItemKNNRankingModelTestCase.class,

        LDAModelTestCase.class,

        LambdaFMModelTestCase.class,

        ListwiseMFModelTestCase.class,

        PLSAModelTestCase.class,

        RankALSModelTestCase.class,

        RankCDModelTestCase.class,

        RankSGDModelTestCase.class,

        RankVFCDModelTestCase.class,

        SLIMModelTestCase.class,

        UserKNNRankingModelTestCase.class,

        VBPRModelTestCase.class,

        WBPRModelTestCase.class,

        WRMFModelTestCase.class, })
public class CollaborativeRankingTestSuite {

}
