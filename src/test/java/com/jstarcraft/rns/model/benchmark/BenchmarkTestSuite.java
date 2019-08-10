package com.jstarcraft.rns.model.benchmark;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.model.benchmark.ranking.MostPopularModelTestCase;
import com.jstarcraft.rns.model.benchmark.rating.ConstantGuessModelTestCase;
import com.jstarcraft.rns.model.benchmark.rating.GlobalAverageModelTestCase;
import com.jstarcraft.rns.model.benchmark.rating.ItemAverageModelTestCase;
import com.jstarcraft.rns.model.benchmark.rating.ItemClusterModelTestCase;
import com.jstarcraft.rns.model.benchmark.rating.UserAverageModelTestCase;
import com.jstarcraft.rns.model.benchmark.rating.UserClusterModelTestCase;

@RunWith(Suite.class)
@SuiteClasses({
		// 推荐器测试集
		ConstantGuessModelTestCase.class,

		GlobalAverageModelTestCase.class,

		ItemAverageModelTestCase.class,

		ItemClusterModelTestCase.class,

		MostPopularModelTestCase.class,

		RandomGuessModelTestCase.class,

		UserAverageModelTestCase.class,

		UserClusterModelTestCase.class })
public class BenchmarkTestSuite {

}
