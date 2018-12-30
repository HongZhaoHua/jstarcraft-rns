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
import com.jstarcraft.recommendation.data.splitter.KFoldCrossValidationSplitter;

public class KFoldCrossValidationSplitterTestCase {

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
		String path = configuration.getString("dfs.data.dir") + "/test/datamodeltest/matrix4by4A.txt";
		Map<String, Integer> fields = new HashMap<>();
		fields.put("user", 0);
		fields.put("item", 1);
		fields.put("score", 2);
		CsvConvertor csvConvertor = new CsvConvertor("csv", ' ', path, fields);
		int count = csvConvertor.convert(space);

		// 制造数据模型
		InstanceAccessor model = space.makeModule("model", "user", "item", "score");

		DataSplitter splitter = new KFoldCrossValidationSplitter(model, 6);
		assertEquals(6, splitter.getSize());
		assertEquals(count, model.getSize());
		for (int index = 0; index < 6; index++) {
			assertEquals(10, splitter.getTrainReference(index).getSize());
			assertEquals(2, splitter.getTestReference(index).getSize());
		}
	}

}
