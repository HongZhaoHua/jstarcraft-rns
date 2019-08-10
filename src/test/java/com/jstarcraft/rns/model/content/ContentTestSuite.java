package com.jstarcraft.rns.model.content;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.model.content.ranking.TFIDFModelTestCase;
import com.jstarcraft.rns.model.content.rating.HFTModelTestCase;
import com.jstarcraft.rns.model.content.rating.TopicMFATModelTestCase;
import com.jstarcraft.rns.model.content.rating.TopicMFMTModelTestCase;

@RunWith(Suite.class)
@SuiteClasses({
		// 推荐器测试集
		EFMModelTestCase.class,

		HFTModelTestCase.class,

		TFIDFModelTestCase.class,

		TopicMFATModelTestCase.class,

		TopicMFMTModelTestCase.class })
public class ContentTestSuite {

}
