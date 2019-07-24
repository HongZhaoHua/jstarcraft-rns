package com.jstarcraft.rns.search.converter.sort;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.rns.search.annotation.SearchSort;
import com.jstarcraft.rns.search.converter.SearchContext;
import com.jstarcraft.rns.search.converter.SortConverter;
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 数组排序转换器
 * 
 * @author Birdy
 *
 */
public class ArraySortConverter implements SortConverter {

    @Override
    public Iterable<IndexableField> convert(SearchContext context, String path, Field field, SearchSort annotation, Type type, Object data) {
        Collection<IndexableField> indexables = new LinkedList<>();
        Class<?> componentClass = null;
        Type componentType = null;
        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = GenericArrayType.class.cast(type);
            componentType = genericArrayType.getGenericComponentType();
            componentClass = TypeUtility.getRawType(componentType, null);
        } else {
            Class<?> clazz = TypeUtility.getRawType(type, null);
            componentType = clazz.getComponentType();
            componentClass = clazz.getComponentType();
        }
        throw new SearchException();
    }

}
