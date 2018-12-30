package com.jstarcraft.recommendation.data.splitter;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.InstanceAccessor;
import com.jstarcraft.recommendation.data.convertor.CsvConvertor;
import com.jstarcraft.recommendation.data.splitter.DataSplitter;
import com.jstarcraft.recommendation.data.splitter.GivenDataSplitter;

public class GivenDataSplitterTestCase {

	@Test
	public void test() {
		Map<String, Class<?>> discreteFeatures = new HashMap<>();
		Set<String> continuousFeatures = new HashSet<>();
		discreteFeatures.put("user", int.class);
		discreteFeatures.put("item", int.class);
		continuousFeatures.add("score");
		DataSpace space = new DataSpace(discreteFeatures, continuousFeatures);

		// 制造数据特征
		space.makeFeature("user", "user");
		space.makeFeature("item", "item");
		space.makeFeature("score", "score");

		Configuration configuration = Configuration.valueOf();
		String trainPath = configuration.getString("dfs.data.dir") + "/test/given-testset/train";
		Map<String, Integer> fields = new HashMap<>();
		fields.put("user", 0);
		fields.put("item", 1);
		fields.put("score", 2);
		CsvConvertor trainConvertor = new CsvConvertor("train", ' ', trainPath, fields);
		int trainCount = trainConvertor.convert(space);
		String testPath = configuration.getString("dfs.data.dir") + "/test/given-testset/test";
		CsvConvertor testConvertor = new CsvConvertor("test", ' ', testPath, fields);
		int testCount = testConvertor.convert(space);

		// 制造数据模型
		InstanceAccessor model = space.makeModule("model", "user", "item", "score");

		DataSplitter splitter = new GivenDataSplitter(model, trainCount);
		assertEquals(1, splitter.getSize());
		assertEquals(trainCount, splitter.getTrainReference(0).getSize());
		assertEquals(testCount, splitter.getTestReference(0).getSize());
		assertEquals(trainCount + testCount, model.getSize());
	}

}
