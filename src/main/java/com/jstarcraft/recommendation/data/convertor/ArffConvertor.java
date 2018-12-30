package com.jstarcraft.recommendation.data.convertor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;

import com.jstarcraft.ai.data.exception.DataException;
import com.jstarcraft.core.utility.StringUtility;
import com.jstarcraft.recommendation.data.DataAttribute;
import com.jstarcraft.recommendation.data.DataFeature;
import com.jstarcraft.recommendation.data.DataSpace;

/**
 * Attribute-Relation File Format转换器
 * 
 * <pre>
 * ARFF定义(http://www.cs.waikato.ac.nz/ml/weka/arff.html)
 * </pre>
 * 
 * @author Birdy
 *
 */
// TODO 准备支持稀疏数据 https://www.bbsmax.com/A/x9J2RnqeJ6/
public class ArffConvertor extends CsvConvertor {

	public ArffConvertor(String name, String path, Map<String, Integer> fields) {
		super(name, CSVFormat.DEFAULT.getDelimiter(), path, fields);
	}

	@Override
	public int convert(DataSpace space) {
		try {
			int count = 0;
			Map<Integer, DataFeature<?>> features = new HashMap<>();
			for (Entry<String, Integer> term : fields.entrySet()) {
				features.put(term.getValue(), space.getFeature(term.getKey()));
			}
			for (File file : files) {
				boolean dataMark = false;
				int attributeIndex = 0;
				try (FileReader reader = new FileReader(file); BufferedReader buffer = new BufferedReader(reader)) {
					while (true) {
						if (dataMark) {
							count += parseData(buffer, features);
							break;
						} else {
							String line = buffer.readLine();
							if (StringUtility.isBlank(line) || line.startsWith("%")) {
								continue;
							}
							String[] datas = line.trim().split("[ \t]");
							switch (datas[0].toUpperCase()) {
							case "@RELATION": {
								break;
							}
							case "@ATTRIBUTE": {
								DataAttribute<?> attribute = features.get(attributeIndex++).getAttribute();
								String attributeType = datas[2];
								if (attributeType.startsWith("{") && attributeType.endsWith("}")) {
									String nominals = attributeType.substring(1, attributeType.length() - 1);
									for (String nominal : nominals.split(",")) {
										attribute.makeValue(nominal);
									}
								}
								break;
							}
							case "@DATA": {
								dataMark = true;
								break;
							}
							}
						}
					}
				}
			}
			return count;
		} catch (Exception exception) {
			// TODO 处理日志.
			throw new DataException(exception);
		}
	}

}