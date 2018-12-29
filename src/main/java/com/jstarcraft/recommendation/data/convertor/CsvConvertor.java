package com.jstarcraft.recommendation.data.convertor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.jstarcraft.ai.data.exception.DataException;
import com.jstarcraft.recommendation.data.DataFeature;
import com.jstarcraft.recommendation.data.DataSpace;

/**
 * Comma-Separated Values转换器
 * 
 * @author Birdy
 *
 */
public class CsvConvertor extends FileConvertor<Integer> {

	/** 分隔符 */
	protected char delimiter;

	public CsvConvertor(String name, char delimiter, String path, Map<String, Integer> fields) {
		super(name, path, fields);
		this.delimiter = delimiter;
	}

	protected int parseData(BufferedReader buffer, Map<Integer, DataFeature<?>> features) throws IOException {
		int count = 0;
		try (CSVParser parser = new CSVParser(buffer, CSVFormat.newFormat(delimiter))) {
			Iterator<CSVRecord> iterator = parser.iterator();
			while (iterator.hasNext()) {
				CSVRecord values = iterator.next();
				if (values.size() < features.size()) {
					// TODO 考虑改为异常或者日志.
					continue;
				}
				for (Entry<Integer, DataFeature<?>> term : features.entrySet()) {
					term.getValue().associate(values.get(term.getKey()));
				}
				count++;
			}
			return count;
		} catch (Exception exception) {
			// TODO 处理日志.
			throw new RuntimeException(exception);
		}
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
