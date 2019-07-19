package com.jstarcraft.rns.search.converter.index;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.converter.IndexConverter;

/**
 * 集合转换器
 * 
 * @author Birdy
 *
 */
public class CollectionIndexConverter implements IndexConverter {

    @Override
    public Iterable<IndexableField> convert(Map<Class<?>, List<KeyValue<Field, IndexConverter>>> context, String path, Field field, SearchIndex annotation, String name, Type type, Object data) {
        ParameterizedType parameterizedType = ParameterizedType.class.cast(type);
        Type[] types = parameterizedType.getActualTypeArguments();
        return null;
    }

}
