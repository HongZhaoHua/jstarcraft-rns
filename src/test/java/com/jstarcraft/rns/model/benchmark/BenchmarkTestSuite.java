package com.jstarcraft.rns.model.benchmark;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.model.benchmark.ranking.MostPopularRecommenderTestCase;
import com.jstarcraft.rns.model.benchmark.rating.ConstantGuessRecommenderTestCase;
import com.jstarcraft.rns.model.benchmark.rating.GlobalAverageRecommenderTestCase;
import com.jstarcraft.rns.model.benchmark.rating.ItemAverageRecommenderTestCase;
import com.jstarcraft.rns.model.benchmark.rating.ItemClusterRecommenderTestCase;
import com.jstarcraft.rns.model.benchmark.rating.UserAverageRecommenderTestCase;
import com.jstarcraft.rns.model.benchmark.rating.UserClusterRecommenderTestCase;

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
