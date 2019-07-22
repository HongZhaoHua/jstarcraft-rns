package com.jstarcraft.rns.search.converter.store;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.Specification;
import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.rns.search.annotation.SearchStore;
import com.jstarcraft.rns.search.converter.SearchContext;
import com.jstarcraft.rns.search.converter.StoreConverter;

/**
 * 数组存储转换器
 * 
 * @author Birdy
 *
 */
// TODO 代码需要重构,避免字符串拼接.
public class ArrayStoreConverter implements StoreConverter {

    @Override
    public Object decode(SearchContext context, String path, Field field, SearchStore annotation, Type type, NavigableMap<String, IndexableField> document) {
        String from = path;
        char character = path.charAt(path.length() - 1);
        character++;
        String to = path.substring(0, path.length() - 1) + character;
        document = document.subMap(from, true, to, false);
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
        Specification specification = Specification.getSpecification(componentClass);
        StoreConverter converter = context.getStoreConverter(specification);
        IndexableField indexable = document.get(path + ".size");
        int size = indexable.numericValue().intValue();
        Object array = Array.newInstance(componentClass, size);
        for (int index = 0; index < size; index++) {
            Object element = converter.decode(context, path + "[" + index + "]", field, annotation, componentType, document);
            Array.set(array, index, element);
        }
        return array;
    }

    @Override
    public NavigableMap<String, IndexableField> encode(SearchContext context, String path, Field field, SearchStore annotation, Type type, Object data) {
        NavigableMap<String, IndexableField> indexables = new TreeMap<>();
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
        Specification specification = Specification.getSpecification(componentClass);
        StoreConverter converter = context.getStoreConverter(specification);
        int size = Array.getLength(data);
        IndexableField indexable = new StoredField(path + ".size", size);
        indexables.put(path + ".size", indexable);
        for (int index = 0; index < size; index++) {
            Object element = Array.get(data, index);
            indexables.putAll(converter.encode(context, path + "[" + index + "]", field, annotation, componentType, element));
        }
        return indexables;
    }

}
