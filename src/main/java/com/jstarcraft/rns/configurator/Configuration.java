package com.jstarcraft.rns.configurator;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.jstarcraft.core.utility.StringUtility;
import com.jstarcraft.rns.recommender.exception.RecommendException;

/**
 * 配置
 * 
 * @author Birdy
 *
 */
@Deprecated
// TODO 准备分离数据配置与算法配置
// TODO 准备重构为基于jstarcraft-core-storage
public class Configuration {

	/** 类容器(用于获取Properties配置) */
	private static ClassLoader container;

	/** 全局配置项 */
	private static Properties global = new Properties();

	static {
		container = Thread.currentThread().getContextClassLoader();
		if (container == null) {
			container = Configuration.class.getClassLoader();
		}
		URL url = container.getResource("data.properties");
		try {
			global.load(url.openStream());
		} catch (Exception exception) {
			throw new RecommendException("无法获取全局配置", exception);
		}
	}

	/** 具体配置项 */
	private Properties property;

	private Configuration(Properties... properties) {
		this.property = new Properties();
		for (Properties property : properties) {
			for (Entry<Object, Object> element : property.entrySet()) {
				this.property.put(element.getKey(), element.getValue());
			}
		}
	}

	public Boolean getBoolean(String name, Boolean defaultValue) {
		String value = getString(name);
		return StringUtility.isBlank(value) ? defaultValue : Boolean.valueOf(value);
	}

	public Boolean getBoolean(String name) {
		return getBoolean(name, null);
	}

	public Character getCharacter(String name, Character defaultValue) {
		String value = getString(name);
		return StringUtility.isBlank(value) ? defaultValue : Character.valueOf(value.charAt(0));
	}

	public Character getCharacter(String name) {
		return getCharacter(name, null);
	}

	public Double getDouble(String name, Double defaultValue) {
		String value = getString(name);
		return StringUtility.isBlank(value) ? defaultValue : Double.valueOf(value);
	}

	public Double getDouble(String name) {
		return getDouble(name, null);
	}

	public Float getFloat(String name, Float defaultValue) {
		String value = getString(name);
		return StringUtility.isBlank(value) ? defaultValue : Float.valueOf(value);
	}

	public Float getFloat(String name) {
		return getFloat(name, null);
	}

	public Integer getInteger(String name, Integer defaultValue) {
		String value = getString(name);
		return StringUtility.isBlank(value) ? defaultValue : Integer.valueOf(value);
	}

	public Integer getInteger(String name) {
		return getInteger(name, null);
	}

	public Long getLong(String name, Long defaultValue) {
		String value = getString(name);
		return StringUtility.isBlank(value) ? defaultValue : Long.valueOf(value);
	}

	public Long getLong(String name) {
		return getLong(name, null);
	}

	public String getString(String name, String defaultValue) {
		String value = getString(name);
		return StringUtility.isBlank(value) ? defaultValue : value;
	}

	public String getString(String name) {
		return property.getProperty(name);
	}

	public static Configuration valueOf() {
		Configuration value = new Configuration(global);
		return value;
	}

	public static Configuration valueOf(Map<String, String> keyValues) {
		Configuration value = new Configuration(global);
		for (Entry<String, String> keyValue : keyValues.entrySet()) {
			value.property.setProperty(keyValue.getKey(), keyValue.getValue());
		}
		return value;
	}

	public static Configuration valueOf(Properties property) {
		Configuration value = new Configuration(global, property);
		return value;
	}

	public static Configuration valueOf(InputStream stream) {
		Properties property = new Properties();
		try {
			property.load(stream);
			return Configuration.valueOf(property);
		} catch (Exception exception) {
			throw new RecommendException("无法获取指定配置", exception);
		}
	}

	public static Configuration valueOf(URL url) {
		try {
			return Configuration.valueOf(url.openStream());
		} catch (Exception exception) {
			throw new RecommendException("无法获取指定配置", exception);
		}
	}

	public static Configuration valueOf(String path) {
		return Configuration.valueOf(container.getResource(path));
	}

}
