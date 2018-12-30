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
import com.jstarcraft.recommendation.data.splitter.LeaveOneCrossValidationSplitter;

public class LeaveOneCrossValidationSplitterTestCase {

	@Test
	public void testWithinDimension() {
		Map<String, Class<?>> discreteFeatures = new HashMap<>();
		Set<String> continuousFeatures = new HashSet<>();
		discreteFeatures.put("user", int.class);
		discreteFeatures.put("item", int.class);
		discreteFeatures.put("instant", long.class);
		continuousFeatures.add("score");
		DataSpace space = new DataSpace(discreteFeatures, continuousFeatures);

		// 制造数据特征
		space.makeFeature("user", "user");
		space.makeFeature("item", "item");
		space.makeFeature("instant", "instant");
		space.makeFeature("score", "score");

		Configuration configuration = Configuration.valueOf();
		String path = configuration.getString("dfs.data.dir") + "/test/datamodeltest/matrix4by4-date.txt";
		Map<String, Integer> fields = new HashMap<>();
		fields.put("user", 0);
		fields.put("item", 1);
		fields.put("instant", 2);
		fields.put("score", 3);
		CsvConvertor csvConvertor = new CsvConvertor("csv", ' ', path, fields);
		int count = csvConvertor.convert(space);

		// 制造数据模型
		InstanceAccessor model = space.makeModule("model", "user", "item", "instant", "score");

		DataSplitter splitter = new LeaveOneCrossValidationSplitter(model, "user", null);
		assertEquals(1, splitter.getSize());
		assertEquals(9, splitter.getTrainReference(0).getSize());
		assertEquals(4, splitter.getTestReference(0).getSize());
		assertEquals(count, model.getSize());
	}

	@Test
	public void testWithoutDimension() {
		Map<String, Class<?>> discreteFeatures = new HashMap<>();
		Set<String> continuousFeatures = new HashSet<>();
		discreteFeatures.put("user", int.class);
		discreteFeatures.put("item", int.class);
		discreteFeatures.put("instant", long.class);
		continuousFeatures.add("score");
		DataSpace space = new DataSpace(discreteFeatures, continuousFeatures);

		// 制造数据特征
		space.makeFeature("user", "user");
		space.makeFeature("item", "item");
		space.makeFeature("instant", "instant");
		space.makeFeature("score", "score");

		Configuration configuration = Configuration.valueOf();
		String path = configuration.getString("dfs.data.dir") + "/test/datamodeltest/matrix4by4-date.txt";
		Map<String, Integer> fields = new HashMap<>();
		fields.put("user", 0);
		fields.put("item", 1);
		fields.put("instant", 2);
		fields.put("score", 3);
		CsvConvertor csvConvertor = new CsvConvertor("csv", ' ', path, fields);
		int count = csvConvertor.convert(space);

		// 制造数据模型
		InstanceAccessor model = space.makeModule("model", "user", "item", "instant", "score");

		DataSplitter splitter = new LeaveOneCrossValidationSplitter(model, null, null);
		assertEquals(1, splitter.getSize());
		assertEquals(12, splitter.getTrainReference(0).getSize());
		assertEquals(1, splitter.getTestReference(0).getSize());
		assertEquals(count, model.getSize());
	}

}
