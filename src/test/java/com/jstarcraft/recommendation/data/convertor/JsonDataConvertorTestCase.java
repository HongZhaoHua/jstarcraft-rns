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
import com.jstarcraft.recommendation.data.convertor.JsonConvertor;

public class JsonDataConvertorTestCase {

	@Test
	public void testByFile() {
		Map<String, Class<?>> discreteFeatures = new HashMap<>();
		Set<String> continuousFeatures = new HashSet<>();
		discreteFeatures.put("id", int.class);
		discreteFeatures.put("name", String.class);
		discreteFeatures.put("alias", String.class);
		discreteFeatures.put("capacities", Map.class);
		discreteFeatures.put("quality", String.class);
		continuousFeatures.add("level");
		DataSpace space = new DataSpace(discreteFeatures, continuousFeatures);

		// 制造数据特征
		DataFeature<?> idFeature = space.makeFeature("id", "id");
		DataFeature<?> nameFeature = space.makeFeature("name", "name");
		DataFeature<?> aliasFeature = space.makeFeature("alias", "alias");
		DataFeature<?> capacityFeature = space.makeFeature("capacities", "capacities");
		DataFeature<?> qualityFeature = space.makeFeature("quality", "quality");
		DataFeature<?> levelFeature = space.makeFeature("level", "level");

		Configuration configuration = Configuration.valueOf();
		String path = configuration.getString("dfs.data.dir") + "/test/convertor/json/json.txt";
		Map<String, String> fields = new HashMap<>();
		fields.put("id", "id");
		fields.put("name", "name");
		fields.put("alias", "alias");
		fields.put("capacities", "capacities");
		fields.put("quality", "quality");
		fields.put("level", "level");
		JsonConvertor csvConvertor = new JsonConvertor("json", path, fields);
		int count = csvConvertor.convert(space);

		assertEquals(count, idFeature.getSize());
		assertEquals(count, nameFeature.getSize());
		assertEquals(count, aliasFeature.getSize());
		assertEquals(count, capacityFeature.getSize());
		assertEquals(count, qualityFeature.getSize());
		assertEquals(count, levelFeature.getSize());
	}

	@Test
	public void testByDirectory() {
		Map<String, Class<?>> discreteFeatures = new HashMap<>();
		Set<String> continuousFeatures = new HashSet<>();
		discreteFeatures.put("id", int.class);
		discreteFeatures.put("name", String.class);
		discreteFeatures.put("alias", String.class);
		discreteFeatures.put("capacities", Map.class);
		discreteFeatures.put("quality", String.class);
		continuousFeatures.add("level");
		DataSpace space = new DataSpace(discreteFeatures, continuousFeatures);

		// 制造数据特征
		DataFeature<?> idFeature = space.makeFeature("id", "id");
		DataFeature<?> nameFeature = space.makeFeature("name", "name");
		DataFeature<?> aliasFeature = space.makeFeature("alias", "alias");
		DataFeature<?> capacityFeature = space.makeFeature("capacities", "capacities");
		DataFeature<?> qualityFeature = space.makeFeature("quality", "quality");
		DataFeature<?> levelFeature = space.makeFeature("level", "level");

		Configuration configuration = Configuration.valueOf();
		String path = configuration.getString("dfs.data.dir") + "/test/convertor/json/";
		Map<String, String> fields = new HashMap<>();
		fields.put("id", "id");
		fields.put("name", "name");
		fields.put("alias", "alias");
		fields.put("capacities", "capacities");
		fields.put("quality", "quality");
		fields.put("level", "level");
		JsonConvertor jsonConvertor = new JsonConvertor("json", path, fields);
		int count = jsonConvertor.convert(space);

		assertEquals(count, idFeature.getSize());
		assertEquals(count, nameFeature.getSize());
		assertEquals(count, aliasFeature.getSize());
		assertEquals(count, capacityFeature.getSize());
		assertEquals(count, qualityFeature.getSize());
		assertEquals(count, levelFeature.getSize());
	}

}
