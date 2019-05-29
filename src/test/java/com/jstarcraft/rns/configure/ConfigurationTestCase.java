package com.jstarcraft.rns.configure;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Type;
import java.util.LinkedList;

import org.junit.Test;

import com.jstarcraft.core.common.conversion.json.JsonUtility;
import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.KeyValue;

public class ConfigurationTestCase {

	@Test
	public void testConfigurationJson() {
		LinkedList<KeyValue<String, Class<?>>> left = new LinkedList<>();
		left.add(new KeyValue<>("user", String.class));
		left.add(new KeyValue<>("item", int.class));
		left.add(new KeyValue<>("score", double.class));
		left.add(new KeyValue<>("word", String.class));
		String json = JsonUtility.object2String(left);
		Type type = TypeUtility.parameterize(KeyValue.class, String.class, Class.class);
		type = TypeUtility.parameterize(LinkedList.class, type);
		LinkedList<KeyValue<String, Class<?>>> right = JsonUtility.string2Object(json, type);
		assertEquals(left, right);
	}

}
