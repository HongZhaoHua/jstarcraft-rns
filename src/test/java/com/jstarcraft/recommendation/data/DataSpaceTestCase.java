package com.jstarcraft.recommendation.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.recommendation.data.ContinuousAttribute;
import com.jstarcraft.recommendation.data.DataFeature;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.DiscreteAttribute;
import com.jstarcraft.recommendation.data.accessor.InstanceAccessor;

public class DataSpaceTestCase {

	@Test
	public void test() {
		Map<String, Class<?>> discreteDifinitions = new HashMap<>();
		Set<String> continuousDifinitions = new HashSet<>();
		discreteDifinitions.put("user", int.class);
		discreteDifinitions.put("item", int.class);
		continuousDifinitions.add("score");
		DataSpace space = new DataSpace(discreteDifinitions, continuousDifinitions);

		// 获取数据属性
		DiscreteAttribute userAttribute = space.getDiscreteAttribute("user");
		DiscreteAttribute itemAttribute = space.getDiscreteAttribute("item");
		ContinuousAttribute scoreAttribute = space.getContinuousAttribute("score");

		// 制造数据特征
		DataFeature<?> userFeature = space.makeFeature("user", "user");
		DataFeature<?> itemFeature = space.makeFeature("item", "item");
		DataFeature<?> scoreFeature = space.makeFeature("score", "score");

		// 检查关联数据对属性和特征的影响
		userFeature.associate("0");
		itemFeature.associate("0");
		scoreFeature.associate("1D");
		Assert.assertEquals(1, userAttribute.getSize());
		Assert.assertEquals(1, itemAttribute.getSize());
		Object[] scoreRange = scoreAttribute.getDatas();
		Assert.assertTrue(1F == (Float) scoreRange[0]);
		Assert.assertTrue(1F == (Float) scoreRange[1]);
		Assert.assertEquals(1, userFeature.getSize());
		Assert.assertEquals(1, itemFeature.getSize());
		Assert.assertEquals(1, scoreFeature.getSize());

		userFeature.associate("0");
		itemFeature.associate("0");
		scoreFeature.associate("1D");
		Assert.assertEquals(1, userAttribute.getSize());
		Assert.assertEquals(1, itemAttribute.getSize());
		scoreRange = scoreAttribute.getDatas();
		Assert.assertTrue(1F == (Float) scoreRange[0]);
		Assert.assertTrue(1F == (Float) scoreRange[1]);
		Assert.assertEquals(2, userFeature.getSize());
		Assert.assertEquals(2, itemFeature.getSize());
		Assert.assertEquals(2, scoreFeature.getSize());

		userFeature.associate("1");
		itemFeature.associate("1");
		scoreFeature.associate("0D");
		Assert.assertEquals(2, userAttribute.getSize());
		Assert.assertEquals(2, itemAttribute.getSize());
		scoreRange = scoreAttribute.getDatas();
		Assert.assertTrue(0F == (Float) scoreRange[0]);
		Assert.assertTrue(1F == (Float) scoreRange[1]);
		Assert.assertEquals(3, userFeature.getSize());
		Assert.assertEquals(3, itemFeature.getSize());
		Assert.assertEquals(3, scoreFeature.getSize());

		// 制造数据特征
		DataFeature<?> trusterFeature = space.makeFeature("truster", "user");
		DataFeature<?> trusteeFeature = space.makeFeature("trustee", "user");

		// 检查关联数据对属性和特征的影响
		trusterFeature.associate("0");
		trusteeFeature.associate("1");
		Assert.assertEquals(2, userAttribute.getSize());
		Assert.assertEquals(1, trusterFeature.getSize());
		Assert.assertEquals(1, trusteeFeature.getSize());

		trusterFeature.associate("0");
		trusteeFeature.associate("2");
		Assert.assertEquals(3, userAttribute.getSize());
		Assert.assertEquals(2, trusterFeature.getSize());
		Assert.assertEquals(2, trusteeFeature.getSize());

		// 制造数据模型
		InstanceAccessor scoreModel = space.makeModule("score", "user", "item", "score");
		InstanceAccessor socialModel = space.makeModule("social", "truster", "trustee");

		Assert.assertEquals(2, scoreModel.getDiscreteOrder());
		Assert.assertEquals(1, scoreModel.getContinuousOrder());
		Assert.assertEquals(3, scoreModel.getSize());

		Assert.assertEquals(2, socialModel.getDiscreteOrder());
		Assert.assertEquals(0, socialModel.getContinuousOrder());
		Assert.assertEquals(2, socialModel.getSize());
	}

}
