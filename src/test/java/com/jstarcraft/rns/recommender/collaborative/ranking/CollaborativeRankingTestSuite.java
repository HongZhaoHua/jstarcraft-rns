package com.jstarcraft.rns.recommender.collaborative.ranking;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        // 推荐器测试集
        // recommender.cf.ranking
        AOBPRTestCase.class,

        AspectModelRankingTestCase.class,

        BHFreeRankingTestCase.class,

        BPRTestCase.class,

        BUCMRankingTestCase.class,

        CDAETestCase.class,

        CLiMFTestCase.class,

//        DeepCrossTestCase.class,

        DeepFMTestCase.class,

        EALSTestCase.class,

        FISMAUCTestCase.class,

        FISMRMSETestCase.class,

        GBPRTestCase.class,

        HMMTestCase.class,

        ItemBigramTestCase.class,

        ItemKNNRankingTestCase.class,

        LDATestCase.class,

        LambdaFMTestCase.class,

        ListwiseMFTestCase.class,

        PLSATestCase.class,

        RankALSTestCase.class,

        RankCDTestCase.class,

        RankSGDTestCase.class,

        RankVFCDTestCase.class,

        SLIMTestCase.class,

        UserKNNRankingTestCase.class,

        VBPRTestCase.class,

        WBPRTestCase.class,

        WRMFTestCase.class, })
public class CollaborativeRankingTestSuite {

}
