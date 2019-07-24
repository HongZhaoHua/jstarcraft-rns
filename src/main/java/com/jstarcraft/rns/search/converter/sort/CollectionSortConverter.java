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
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 集合排序转换器
 * 
 * @author Birdy
 *
 */
public class CollectionSortConverter implements SortConverter {

    @Override
    public Iterable<IndexableField> convert(SearchContext context, String path, Field field, SearchSort annotatio, Type type, Object data) {
        Collection<IndexableField> indexables = new LinkedList<>();
        ParameterizedType parameterizedType = ParameterizedType.class.cast(type);
        Type[] types = parameterizedType.getActualTypeArguments();
        throw new SearchException();
    }

}
