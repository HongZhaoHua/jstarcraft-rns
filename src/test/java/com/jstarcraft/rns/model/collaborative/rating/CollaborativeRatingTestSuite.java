package com.jstarcraft.rns.model.collaborative.rating;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        // 推荐器测试集
        // recommender.cf.rating
        AspectModelRatingModelTestCase.class,

        ASVDPlusPlusModelTestCase.class,

        BiasedMFModelTestCase.class,

        BHFreeRatingModelTestCase.class,

        BPMFModelTestCase.class,

        BUCMRatingModelTestCase.class,

        CCDModelTestCase.class,

        FFMModelTestCase.class,

        FMALSModelTestCase.class,

        FMSGDModelTestCase.class,

        GPLSAModelTestCase.class,

        IRRGModelTestCase.class,

        ItemKNNRatingModelTestCase.class,

        LDCCModelTestCase.class,

        LLORMAModelTestCase.class,

        MFALSModelTestCase.class,

        NMFModelTestCase.class,

        PMFModelTestCase.class,

        RBMModelTestCase.class,

        RFRecModelTestCase.class,

        SVDPlusPlusModelTestCase.class,

        URPModelTestCase.class,

        UserKNNRatingModelTestCase.class, })
public class CollaborativeRatingTestSuite {

}
