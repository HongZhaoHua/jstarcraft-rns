package com.jstarcraft.recommendation.data.convertor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.jstarcraft.ai.data.exception.DataException;
import com.jstarcraft.core.utility.JsonUtility;
import com.jstarcraft.core.utility.StringUtility;
import com.jstarcraft.recommendation.data.DataFeature;
import com.jstarcraft.recommendation.data.DataSpace;

/**
 * JavaScript Object Notation转换器
 * 
 * @author Birdy
 *
 */
public class JsonConvertor extends FileConvertor<String> {

	public JsonConvertor(String name, String path, Map<String, String> fields) {
		super(name, path, fields);
	}

	protected int parseData(BufferedReader buffer, Map<String, DataFeature<?>> features) throws IOException {
		int count = 0;
		String line = null;
		while ((line = buffer.readLine()) != null) {
			if (StringUtility.isBlank(line)) {
				// TODO 考虑改为异常或者日志.
				continue;
			}
			Map<String, Object> data = JsonUtility.string2Object(line, Map.class);
			for (Entry<String, DataFeature<?>> term : features.entrySet()) {
				term.getValue().associate(data.get(term.getKey()));
			}
			count++;
		}
		return count;
	}

	@Override
	public int convert(DataSpace space) {
		try {
			int count = 0;
			Map<String, DataFeature<?>> features = new HashMap<>();
			for (Entry<String, String> term : fields.entrySet()) {
				features.put(term.getValue(), space.getFeature(term.getKey()));
			}
			for (File file : files) {
				try (FileReader reader = new FileReader(file); BufferedReader buffer = new BufferedReader(reader)) {
					count += parseData(buffer, features);
				}
			}
			return count;
		} catch (Exception exception) {
			// TODO 处理日志.
			throw new DataException(exception);
		}
	}

}
