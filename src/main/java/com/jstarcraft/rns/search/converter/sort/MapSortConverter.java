package com.jstarcraft.rns.search.converter.sort;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.rns.search.annotation.SearchSort;
import com.jstarcraft.rns.search.converter.SearchContext;
import com.jstarcraft.rns.search.converter.SortConverter;

/**
 * 映射排序转换器
 * 
 * @author Birdy
 *
 */
public class MapSortConverter implements SortConverter {

    @Override
    public Iterable<IndexableField> convert(SearchContext context, String path, Field field, SearchSort annotation, Type type, Object data) {
        Collection<IndexableField> fields = new LinkedList<>();
        ParameterizedType parameterizedType = ParameterizedType.class.cast(type);
        Type[] types = parameterizedType.getActualTypeArguments();
        return null;
    }

}
