package com.jstarcraft.rns.search.converter.store;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.ReflectionUtility;
import com.jstarcraft.core.common.reflection.Specification;
import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchStore;
import com.jstarcraft.rns.search.converter.SearchCodec;
import com.jstarcraft.rns.search.converter.StoreConverter;

/**
 * 集合存储转换器
 * 
 * @author Birdy
 *
 */
public class CollectionStoreConverter implements StoreConverter {

    @Override
    public Object decode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, Type type, NavigableMap<String, IndexableField> document) {
        String from = path;
        char character = path.charAt(path.length() - 1);
        character++;
        String to = path.substring(0, path.length() - 1) + character;
        document = document.subMap(from, true, to, false);
        Class<?> clazz = TypeUtility.getRawType(type, null);
        // 兼容UniMi
        type = TypeUtility.refineType(type, Collection.class);
        ParameterizedType parameterizedType = ParameterizedType.class.cast(type);
        Type[] types = parameterizedType.getActualTypeArguments();
        Type elementType = types[0];
        Class<?> elementClazz = TypeUtility.getRawType(elementType, null);

        // TODO 此处需要代码重构
        Collection<Object> collection = (Collection<Object>) ReflectionUtility.getInstance(clazz);
        Specification specification = Specification.getSpecification(elementClazz);
        StoreConverter converter = SearchCodec.STORE_CONVERTERS.get(specification);

        IndexableField indexable = document.get(path + ".size");
        int size = indexable.numericValue().intValue();
        for (int index = 0; index < size; index++) {
            Object element = converter.decode(context, path + "[" + index + "]", field, annotation, elementType, document);
            collection.add(element);
        }
        return collection;
    }

    @Override
    public NavigableMap<String, IndexableField> encode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, Type type, Object data) {
        NavigableMap<String, IndexableField> indexables = new TreeMap<>();
        // 兼容UniMi
        type = TypeUtility.refineType(type, Collection.class);
        ParameterizedType parameterizedType = ParameterizedType.class.cast(type);
        Type[] types = parameterizedType.getActualTypeArguments();
        Type elementType = types[0];
        Class<?> elementClazz = TypeUtility.getRawType(elementType, null);

        Collection<?> collection = Collection.class.cast(data);
        Specification specification = Specification.getSpecification(elementClazz);
        StoreConverter converter = SearchCodec.STORE_CONVERTERS.get(specification);

        int size = collection.size();
        IndexableField indexable = new StoredField(path + ".size", size);
        indexables.put(path + ".size", indexable);
        int index = 0;
        for (Object element : collection) {
            indexables.putAll(converter.encode(context, path + "[" + index++ + "]", field, annotation, elementType, element));
        }
        return indexables;
    }

}
