package com.jstarcraft.rns.recommend.content;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.recommend.content.ranking.TFIDFRecommenderTestCase;
import com.jstarcraft.rns.recommend.content.rating.HFTRecommenderTestCase;
import com.jstarcraft.rns.recommend.content.rating.TopicMFATRecommenderTestCase;
import com.jstarcraft.rns.recommend.content.rating.TopicMFMTRecommenderTestCase;

@RunWith(Suite.class)
@SuiteClasses({
		// 推荐器测试集
		EFMRecommenderTestCase.class,

		HFTRecommenderTestCase.class,

		TFIDFRecommenderTestCase.class,

		TopicMFATRecommenderTestCase.class,

		TopicMFMTRecommenderTestCase.class })
public class ContentTestSuite {

}
