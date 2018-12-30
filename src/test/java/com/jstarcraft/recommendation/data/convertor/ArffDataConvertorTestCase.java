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
import com.jstarcraft.recommendation.data.convertor.ArffConvertor;

public class ArffDataConvertorTestCase {

	@Test
	public void testByFile() {
		Map<String, Class<?>> discreteFeatures = new HashMap<>();
		Set<String> continuousFeatures = new HashSet<>();
		discreteFeatures.put("user", int.class);
		discreteFeatures.put("item", int.class);
		discreteFeatures.put("instant", long.class);
		discreteFeatures.put("geography", String.class);
		continuousFeatures.add("score");
		DataSpace space = new DataSpace(discreteFeatures, continuousFeatures);

		// 制造数据特征
		DataFeature<?> userFeature = space.makeFeature("user", "user");
		DataFeature<?> itemFeature = space.makeFeature("item", "item");
		DataFeature<?> instantFeature = space.makeFeature("instant", "instant");
		DataFeature<?> geographyFeature = space.makeFeature("geography", "geography");
		DataFeature<?> scoreFeature = space.makeFeature("score", "score");

		Configuration configuration = Configuration.valueOf();
		String path = configuration.getString("dfs.data.dir") + "/test/arfftest/data.arff";
		Map<String, Integer> fields = new HashMap<>();
		fields.put("user", 0);
		fields.put("item", 1);
		fields.put("instant", 2);
		fields.put("geography", 3);
		fields.put("score", 4);
		ArffConvertor arffConvertor = new ArffConvertor("arff", path, fields);
		int count = arffConvertor.convert(space);

		assertEquals(count, userFeature.getSize());
		assertEquals(count, itemFeature.getSize());
		assertEquals(count, instantFeature.getSize());
		assertEquals(count, geographyFeature.getSize());
		assertEquals(count, scoreFeature.getSize());
	}

	@Test
	public void testByDirectory() {
		Map<String, Class<?>> discreteFeatures = new HashMap<>();
		Set<String> continuousFeatures = new HashSet<>();
		discreteFeatures.put("user", int.class);
		discreteFeatures.put("item", int.class);
		discreteFeatures.put("instant", long.class);
		discreteFeatures.put("geography", String.class);
		continuousFeatures.add("score");
		DataSpace space = new DataSpace(discreteFeatures, continuousFeatures);

		// 制造数据特征
		DataFeature<?> userFeature = space.makeFeature("user", "user");
		DataFeature<?> itemFeature = space.makeFeature("item", "item");
		DataFeature<?> instantFeature = space.makeFeature("instant", "instant");
		DataFeature<?> geographyFeature = space.makeFeature("geography", "geography");
		DataFeature<?> scoreFeature = space.makeFeature("score", "score");

		Configuration configuration = Configuration.valueOf();
		String path = configuration.getString("dfs.data.dir") + "/test/arfftest";
		Map<String, Integer> fields = new HashMap<>();
		fields.put("user", 0);
		fields.put("item", 1);
		fields.put("instant", 2);
		fields.put("geography", 3);
		fields.put("score", 4);
		ArffConvertor arffConvertor = new ArffConvertor("arff", path, fields);
		int count = arffConvertor.convert(space);

		assertEquals(count, userFeature.getSize());
		assertEquals(count, itemFeature.getSize());
		assertEquals(count, instantFeature.getSize());
		assertEquals(count, geographyFeature.getSize());
		assertEquals(count, scoreFeature.getSize());
	}
}