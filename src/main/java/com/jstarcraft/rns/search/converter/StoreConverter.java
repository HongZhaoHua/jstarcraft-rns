package com.jstarcraft.rns.search.converter;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchStore;

/**
 * 存储转换器
 * 
 * @author Birdy
 *
 */
public interface StoreConverter {

    /**
     * 编码存储
     * 
     * @param context
     * @param path
     * @param field
     * @param annotation
     * @param name
     * @param type
     * @param data
     * @return
     */
    NavigableMap<String, IndexableField> encode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, Type type, Object data);

    /**
     * 解码存储
     * 
     * @param context
     * @param path
     * @param field
     * @param annotation
     * @param name
     * @param type
     * @param document
     * @return
     */
    Object decode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, Type type, NavigableMap<String, IndexableField> document);

}
