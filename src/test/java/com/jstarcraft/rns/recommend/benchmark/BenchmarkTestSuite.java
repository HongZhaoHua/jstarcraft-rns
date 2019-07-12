package com.jstarcraft.rns.recommend.benchmark;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.recommend.benchmark.ranking.MostPopularTestCase;
import com.jstarcraft.rns.recommend.benchmark.rating.ConstantGuessTestCase;
import com.jstarcraft.rns.recommend.benchmark.rating.GlobalAverageTestCase;
import com.jstarcraft.rns.recommend.benchmark.rating.ItemAverageTestCase;
import com.jstarcraft.rns.recommend.benchmark.rating.ItemClusterTestCase;
import com.jstarcraft.rns.recommend.benchmark.rating.UserAverageTestCase;
import com.jstarcraft.rns.recommend.benchmark.rating.UserClusterTestCase;

@RunWith(Suite.class)
@SuiteClasses({
		// 推荐器测试集
		ConstantGuessTestCase.class,

		GlobalAverageTestCase.class,

		ItemAverageTestCase.class,

		ItemClusterTestCase.class,

		MostPopularTestCase.class,

		RandomGuessTestCase.class,

		UserAverageTestCase.class,

		UserClusterTestCase.class })
public class BenchmarkTestSuite {

}
