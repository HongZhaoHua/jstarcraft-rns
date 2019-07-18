package com.jstarcraft.rns.search.index;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.rns.search.annotation.SearchEncode;
import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.sort.SortConverter;

/**
 * 集合转换器
 * 
 * @author Birdy
 *
 */
public class CollectionConverter implements IndexConverter {

    @Override
    public Iterable<IndexableField> convert(Map<Field, SortConverter> context, String path, Field field, SearchIndex annotation, String name, Type type, Object data) {
        ParameterizedType parameterizedType = ParameterizedType.class.cast(type);
        Type[] types = parameterizedType.getActualTypeArguments();
        return null;
    }

}
