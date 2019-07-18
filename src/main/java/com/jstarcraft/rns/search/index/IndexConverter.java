package com.jstarcraft.rns.search.index;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Map;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.sort.SortConverter;

/**
 * 索引转换器
 * 
 * @author Birdy
 *
 */
public interface IndexConverter {

    /**
     * 转换索引
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
    Iterable<IndexableField> convert(Map<Field, SortConverter> context, String path, Field field, SearchIndex annotation, String name, Type type, Object data);

}
