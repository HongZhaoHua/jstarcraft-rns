package com.jstarcraft.recommendation.data.convertor;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.io.FileUtils;

public abstract class FileConvertor<T> extends AbstractConvertor<T> {

	/** 文件 */
	protected Collection<File> files;

	protected FileConvertor(String name, String path, Map<String, T> fields) {
		super(name, fields);
		File dataPath = new File(path);
		if (dataPath.isDirectory()) {
			this.files = FileUtils.listFiles(dataPath, null, true);
		} else {
			this.files = Arrays.asList(dataPath);
		}
	}

}
