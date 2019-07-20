package com.jstarcraft.rns.search.converter.sort;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchSort;
import com.jstarcraft.rns.search.converter.SortConverter;

/**
 * 映射转换器
 * 
 * @author Birdy
 *
 */
public class MapSortConverter implements SortConverter {

    @Override
    public Iterable<IndexableField> convert(Map<Class<?>, List<KeyValue<Field, SortConverter>>> context, String path, Field field, SearchSort annotation, String name, Type type, Object data) {
        Collection<IndexableField> fields = new LinkedList<>();
        name = path  + name;
        ParameterizedType parameterizedType = ParameterizedType.class.cast(type);
        Type[] types = parameterizedType.getActualTypeArguments();
        return null;
    }

}
