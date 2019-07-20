package com.jstarcraft.rns.search.converter.store;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.Specification;
import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchStore;
import com.jstarcraft.rns.search.converter.SearchCodec;
import com.jstarcraft.rns.search.converter.StoreConverter;

/**
 * 数组存储转换器
 * 
 * @author Birdy
 *
 */
public class ArrayStoreConverter implements StoreConverter {

    @Override
    public NavigableMap<String, IndexableField> encode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, Type type, Object data) {
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
        StoreConverter converter = SearchCodec.STORE_CONVERTERS.get(specification);
        int size = Array.getLength(data);
        for (int index = 0; index < size; index++) {
            Object element = Array.get(data, index);
            indexables.putAll(converter.encode(context, path + "[" + index + "]", field, annotation, componentClass, element));
        }
        IndexableField indexable = new StoredField(path + ".size", size);
        indexables.put(path + ".size", indexable);
        return indexables;
    }

    @Override
    public Object decode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, Type type, NavigableMap<String, IndexableField> document) {
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
        IndexableField indexable = document.get(path + ".size");
        int size = indexable.numericValue().intValue();
        Object array = Array.newInstance(componentClass, size);
        Specification specification = Specification.getSpecification(componentClass);
        StoreConverter converter = SearchCodec.STORE_CONVERTERS.get(specification);
        for (int index = 0; index < size; index++) {
            Object element = converter.decode(context, path + "[" + index + "]", field, annotation, componentClass, document);
            Array.set(array, index, element);
        }
        return array;
    }

}
