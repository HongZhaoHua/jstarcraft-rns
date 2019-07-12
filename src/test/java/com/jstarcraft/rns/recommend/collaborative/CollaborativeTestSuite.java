package com.jstarcraft.rns.recommend.collaborative;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.jstarcraft.rns.recommend.collaborative.ranking.CollaborativeRankingTestSuite;
import com.jstarcraft.rns.recommend.collaborative.rating.CollaborativeRatingTestSuite;

@RunWith(Suite.class)
@SuiteClasses({
		// 推荐器测试集
		// recommender.cf.ranking
		CollaborativeRankingTestSuite.class,

		// recommender.cf.rating
		CollaborativeRatingTestSuite.class, })
public class CollaborativeTestSuite {

}
