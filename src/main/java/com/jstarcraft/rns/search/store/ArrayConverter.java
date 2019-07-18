package com.jstarcraft.rns.search.store;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Map;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.rns.search.annotation.SearchStore;
import com.jstarcraft.rns.search.sort.SortConverter;

/**
 * 数组转换器
 * 
 * @author Birdy
 *
 */
public class ArrayConverter implements StoreConverter {

    @Override
    public Iterable<IndexableField> encode(Map<Field, SortConverter> context, String path, Field field, SearchStore annotation, String name, Type type, Object data) {
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

    @Override
    public Object decode(Map<Field, SortConverter> context, String path, Field field, SearchStore annotation, String name, Type type, Iterable<IndexableField> document) {
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
