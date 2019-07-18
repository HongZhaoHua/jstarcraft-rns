package com.jstarcraft.rns.search.sort;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Map;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.rns.search.annotation.SearchSort;

/**
 * 对象转换器
 * 
 * @author Birdy
 *
 */
public class ObjectConverter implements SortConverter {

    @Override
    public Iterable<IndexableField> convert(Map<Field, SortConverter> context, String path, Field field, SearchSort annotation, String name, Type type, Object data) {
        // TODO Auto-generated method stub
        return null;
    }

}
