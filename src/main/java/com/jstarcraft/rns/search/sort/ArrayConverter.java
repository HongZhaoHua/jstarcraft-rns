package com.jstarcraft.rns.search.sort;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Map;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.rns.search.annotation.SearchSort;

/**
 * 数组转换器
 * 
 * @author Birdy
 *
 */
public class ArrayConverter implements SortConverter {

    @Override
    public Iterable<IndexableField> convert(Map<Field, SortConverter> context, String path, Field field, SearchSort annotation, String name, Type type, Object data) {
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
        return null;
    }

}
