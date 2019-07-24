package com.jstarcraft.rns.search.converter.store;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.Specification;
import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.rns.search.annotation.SearchStore;
import com.jstarcraft.rns.search.converter.SearchContext;
import com.jstarcraft.rns.search.converter.StoreConverter;
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 映射存储转换器
 * 
 * @author Birdy
 *
 */
public class MapStoreConverter implements StoreConverter {

    @Override
    public Object decode(SearchContext context, String path, Field field, SearchStore annotation, Type type, NavigableMap<String, IndexableField> indexables) {
        String from = path;
        char character = path.charAt(path.length() - 1);
        character++;
        String to = path.substring(0, path.length() - 1) + character;
        indexables = indexables.subMap(from, true, to, false);
        Class<?> clazz = TypeUtility.getRawType(type, null);
        // 兼容UniMi
        type = TypeUtility.refineType(type, Map.class);
        ParameterizedType parameterizedType = ParameterizedType.class.cast(type);
        Type[] types = parameterizedType.getActualTypeArguments();
        Type keyType = types[0];
        Class<?> keyClazz = TypeUtility.getRawType(keyType, null);
        Type valueType = types[1];
        Class<?> valueClazz = TypeUtility.getRawType(valueType, null);

        try {
            // TODO 此处需要代码重构
            Map<Object, Object> map = (Map) context.getInstance(clazz);
            Specification keySpecification = Specification.getSpecification(keyClazz);
            StoreConverter keyConverter = context.getStoreConverter(keySpecification);
            Specification valueSpecification = Specification.getSpecification(valueClazz);
            StoreConverter valueConverter = context.getStoreConverter(valueSpecification);

            IndexableField indexable = indexables.get(path + ".size");
            int size = indexable.numericValue().intValue();
            for (int index = 0; index < size; index++) {
                Object key = keyConverter.decode(context, path + "[" + index + "_key]", field, annotation, keyType, indexables);
                Object value = valueConverter.decode(context, path + "[" + index + "_value]", field, annotation, valueType, indexables);
                map.put(key, value);
            }
            return map;
        } catch (Exception exception) {
            // TODO
            throw new SearchException(exception);
        }
    }

    @Override
    public NavigableMap<String, IndexableField> encode(SearchContext context, String path, Field field, SearchStore annotation, Type type, Object instance) {
        NavigableMap<String, IndexableField> indexables = new TreeMap<>();
        // 兼容UniMi
        type = TypeUtility.refineType(type, Map.class);
        ParameterizedType parameterizedType = ParameterizedType.class.cast(type);
        Type[] types = parameterizedType.getActualTypeArguments();
        Type keyType = types[0];
        Class<?> keyClazz = TypeUtility.getRawType(keyType, null);
        Type valueType = types[1];
        Class<?> valueClazz = TypeUtility.getRawType(valueType, null);

        try {
            // TODO 此处需要代码重构
            Map<Object, Object> map = Map.class.cast(instance);
            Specification keySpecification = Specification.getSpecification(keyClazz);
            StoreConverter keyConverter = context.getStoreConverter(keySpecification);
            Specification valueSpecification = Specification.getSpecification(valueClazz);
            StoreConverter valueConverter = context.getStoreConverter(valueSpecification);

            int size = map.size();
            IndexableField indexable = new StoredField(path + ".size", size);
            indexables.put(path + ".size", indexable);
            int index = 0;
            for (Entry<Object, Object> keyValue : map.entrySet()) {
                Object key = keyValue.getKey();
                indexables.putAll(keyConverter.encode(context, path + "[" + index + "_key]", field, annotation, keyType, key));
                Object value = keyValue.getValue();
                indexables.putAll(valueConverter.encode(context, path + "[" + index + "_value]", field, annotation, valueType, value));
                index++;
            }
            return indexables;
        } catch (Exception exception) {
            // TODO
            throw new SearchException(exception);
        }
    }

}
