package com.jstarcraft.recommendation.data.convertor;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataFeature;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.convertor.CsvConvertor;

public class CsvDataConvertorTestCase {

	@Test
	public void testByFile() {
		Map<String, Class<?>> discreteFeatures = new HashMap<>();
		Set<String> continuousFeatures = new HashSet<>();
		discreteFeatures.put("user", int.class);
		discreteFeatures.put("item", int.class);
		continuousFeatures.add("score");
		DataSpace space = new DataSpace(discreteFeatures, continuousFeatures);

		// 制造数据特征
		DataFeature<?> userFeature = space.makeFeature("user", "user");
		DataFeature<?> itemFeature = space.makeFeature("item", "item");
		DataFeature<?> scoreFeature = space.makeFeature("score", "score");

		Configuration configuration = Configuration.valueOf();
		String path = configuration.getString("dfs.data.dir") + "/test/test-convert-dir/sytTest4by4.txt";
		Map<String, Integer> fields = new HashMap<>();
		fields.put("user", 0);
		fields.put("item", 1);
		fields.put("score", 2);
		CsvConvertor csvConvertor = new CsvConvertor("csv", ' ', path, fields);
		int count = csvConvertor.convert(space);

		assertEquals(count, userFeature.getSize());
		assertEquals(count, itemFeature.getSize());
		assertEquals(count, scoreFeature.getSize());
	}

	@Test
	public void testByDirectory() {
		Map<String, Class<?>> discreteFeatures = new HashMap<>();
		Set<String> continuousFeatures = new HashSet<>();
		discreteFeatures.put("user", int.class);
		discreteFeatures.put("item", int.class);
		continuousFeatures.add("score");
		DataSpace space = new DataSpace(discreteFeatures, continuousFeatures);

		// 制造数据特征
		DataFeature<?> userFeature = space.makeFeature("user", "user");
		DataFeature<?> itemFeature = space.makeFeature("item", "item");
		DataFeature<?> scoreFeature = space.makeFeature("score", "score");

		Configuration configuration = Configuration.valueOf();
		String path = configuration.getString("dfs.data.dir") + "/test/test-convert-dir/";
		Map<String, Integer> fields = new HashMap<>();
		fields.put("user", 0);
		fields.put("item", 1);
		fields.put("score", 2);
		CsvConvertor csvConvertor = new CsvConvertor("csv", ' ', path, fields);
		int count = csvConvertor.convert(space);

		assertEquals(count, userFeature.getSize());
		assertEquals(count, itemFeature.getSize());
		assertEquals(count, scoreFeature.getSize());
	}

}
