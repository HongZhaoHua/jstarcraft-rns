package com.jstarcraft.rns.recommend.benchmark;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.recommend.benchmark.ranking.MostPopularRecommenderTestCase;
import com.jstarcraft.rns.recommend.benchmark.rating.ConstantGuessRecommenderTestCase;
import com.jstarcraft.rns.recommend.benchmark.rating.GlobalAverageRecommenderTestCase;
import com.jstarcraft.rns.recommend.benchmark.rating.ItemAverageRecommenderTestCase;
import com.jstarcraft.rns.recommend.benchmark.rating.ItemClusterRecommenderTestCase;
import com.jstarcraft.rns.recommend.benchmark.rating.UserAverageRecommenderTestCase;
import com.jstarcraft.rns.recommend.benchmark.rating.UserClusterRecommenderTestCase;

@RunWith(Suite.class)
@SuiteClasses({
		// 推荐器测试集
		ConstantGuessRecommenderTestCase.class,

		GlobalAverageRecommenderTestCase.class,

		ItemAverageRecommenderTestCase.class,

		ItemClusterRecommenderTestCase.class,

		MostPopularRecommenderTestCase.class,

		RandomGuessRecommenderTestCase.class,

		UserAverageRecommenderTestCase.class,

		UserClusterRecommenderTestCase.class })
public class BenchmarkTestSuite {

}
