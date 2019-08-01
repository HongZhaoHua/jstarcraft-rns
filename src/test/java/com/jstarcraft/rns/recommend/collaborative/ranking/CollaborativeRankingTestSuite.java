package com.jstarcraft.rns.recommend.collaborative.ranking;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        // 推荐器测试集
        // recommender.cf.ranking
        AoBPRRecommenderTestCase.class,

        AspectModelRankingRecommenderTestCase.class,

        BHFreeRankingRecommenderTestCase.class,

        BPRRecommenderTestCase.class,

        BUCMRankingRecommenderTestCase.class,

        CDAERecommenderTestCase.class,

        CLiMFRecommenderTestCase.class,

//        DeepCrossTestCase.class,

        DeepFMRecommenderTestCase.class,

        EALSRecommenderTestCase.class,

        FISMAUCRecommenderTestCase.class,

        FISMRMSERecommenderTestCase.class,

        GBPRRecommenderTestCase.class,

        HMMRecommenderTestCase.class,

        ItemBigramRecommenderTestCase.class,

        ItemKNNRankingRecommenderTestCase.class,

        LDARecommenderTestCase.class,

        LambdaFMRecommenderTestCase.class,

        ListwiseMFRecommenderTestCase.class,

        PLSARecommenderTestCase.class,

        RankALSRecommenderTestCase.class,

        RankCDRecommenderTestCase.class,

        RankSGDRecommenderTestCase.class,

        RankVFCDRecommenderTestCase.class,

        SLIMRecommenderTestCase.class,

        UserKNNRankingRecommenderTestCase.class,

        VBPRRecommenderTestCase.class,

        WBPRRecommenderTestCase.class,

        WRMFRecommenderTestCase.class, })
public class CollaborativeRankingTestSuite {

}
