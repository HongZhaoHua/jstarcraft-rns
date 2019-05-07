package com.jstarcraft.rns.recommender;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.recommender.benchmark.BenchmarkTestSuite;
import com.jstarcraft.rns.recommender.collaborative.CollaborativeTestSuite;
import com.jstarcraft.rns.recommender.content.ContentTestSuite;
import com.jstarcraft.rns.recommender.context.ContextTestSuite;
import com.jstarcraft.rns.recommender.extend.ExtendTestSuite;

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
