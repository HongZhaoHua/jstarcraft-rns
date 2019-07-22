package com.jstarcraft.rns.search.converter;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.rns.search.annotation.SearchSort;

/**
 * 排序转换器
 * 
 * @author Birdy
 *
 */
public interface SortConverter {

    /**
     * 转换排序
     * 
     * @param context
     * @param path
     * @param annotation
     * @param field
     * @param data
     * @return
     */
    Iterable<IndexableField> convert(SearchContext context, String path, Field field, SearchSort annotation, Type type, Object data);

}
