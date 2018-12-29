package com.jstarcraft.recommendation.recommender;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.recommendation.recommender.benchmark.BenchmarkTestSuite;
import com.jstarcraft.recommendation.recommender.collaborative.CollaborativeTestSuite;
import com.jstarcraft.recommendation.recommender.content.ContentTestSuite;
import com.jstarcraft.recommendation.recommender.context.ContextTestSuite;
import com.jstarcraft.recommendation.recommender.extend.ExtendTestSuite;

@RunWith(Suite.class)
@SuiteClasses({
		// 推荐器测试集(运行参数:-Xms1024M -Xmx8192M -ea)
		BenchmarkTestSuite.class,

		CollaborativeTestSuite.class,

		ContentTestSuite.class,

		ContextTestSuite.class,

		ExtendTestSuite.class, })
public class RecommenderTestSuite {

}
