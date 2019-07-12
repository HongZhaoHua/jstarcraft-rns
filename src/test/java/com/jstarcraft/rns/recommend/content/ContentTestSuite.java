package com.jstarcraft.rns.recommend.content;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.recommend.content.ranking.TFIDFTestCase;
import com.jstarcraft.rns.recommend.content.rating.HFTTestCase;
import com.jstarcraft.rns.recommend.content.rating.TopicMFATTestCase;
import com.jstarcraft.rns.recommend.content.rating.TopicMFMTTestCase;

@RunWith(Suite.class)
@SuiteClasses({
		// 推荐器测试集
		EFMTestCase.class,

		HFTTestCase.class,

		TFIDFTestCase.class,

		TopicMFATTestCase.class,

		TopicMFMTTestCase.class })
public class ContentTestSuite {

}
