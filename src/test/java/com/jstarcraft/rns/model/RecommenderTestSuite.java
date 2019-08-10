package com.jstarcraft.rns.model;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.model.benchmark.BenchmarkTestSuite;
import com.jstarcraft.rns.model.collaborative.CollaborativeTestSuite;
import com.jstarcraft.rns.model.content.ContentTestSuite;
import com.jstarcraft.rns.model.context.ContextTestSuite;
import com.jstarcraft.rns.model.extend.ExtendTestSuite;

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
