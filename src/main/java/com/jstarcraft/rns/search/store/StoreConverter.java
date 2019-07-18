package com.jstarcraft.rns.search.store;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Map;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.rns.search.annotation.SearchStore;
import com.jstarcraft.rns.search.sort.SortConverter;

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
    Iterable<IndexableField> encode(Map<Field, SortConverter> context, String path, Field field, SearchStore annotation, String name, Type type, Object data);

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
    Object decode(Map<Field, SortConverter> context, String path, Field field, SearchStore annotation, String name, Type type, Iterable<IndexableField> document);

}
