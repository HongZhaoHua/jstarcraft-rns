package com.jstarcraft.rns.search.converter.store;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.ClassUtility;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchStore;
import com.jstarcraft.rns.search.converter.StoreConverter;
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 数值存储转换器
 * 
 * @author Birdy
 *
 */
public class NumberStoreConverter implements StoreConverter {

    @Override
    public NavigableMap<String, IndexableField> encode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, Type type, Object data) {
        NavigableMap<String, IndexableField> indexables = new TreeMap<>();
        Class<?> clazz = TypeUtility.getRawType(type, null);
        clazz = ClassUtility.primitiveToWrapper(clazz);
        if (Byte.class.isAssignableFrom(clazz)) {
            indexables.put(path, new StoredField(path, (Byte) data));
            return indexables;
        }
        if (Short.class.isAssignableFrom(clazz)) {
            indexables.put(path, new StoredField(path, (Short) data));
            return indexables;
        }
        if (Integer.class.isAssignableFrom(clazz)) {
            indexables.put(path, new StoredField(path, (Integer) data));
            return indexables;
        }
        if (Long.class.isAssignableFrom(clazz)) {
            indexables.put(path, new StoredField(path, (Long) data));
            return indexables;
        }
        if (Float.class.isAssignableFrom(clazz)) {
            indexables.put(path, new StoredField(path, (Float) data));
            return indexables;
        }
        if (Double.class.isAssignableFrom(clazz)) {
            indexables.put(path, new StoredField(path, (Double) data));
            return indexables;
        }
        throw new SearchException();
    }

    @Override
    public Object decode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, Type type, NavigableMap<String, IndexableField> document) {
        String from = path;
        char character = path.charAt(path.length() - 1);
        character++;
        String to = path.substring(0, path.length() - 1) + character;
        document = document.subMap(from, true, to, false);
        IndexableField indexable = document.firstEntry().getValue();
        Class<?> clazz = TypeUtility.getRawType(type, null);
        clazz = ClassUtility.primitiveToWrapper(clazz);
        Number number = indexable.numericValue();
        if (Byte.class.isAssignableFrom(clazz)) {
            return number.byteValue();
        }
        if (Short.class.isAssignableFrom(clazz)) {
            return number.shortValue();
        }
        if (Integer.class.isAssignableFrom(clazz)) {
            return number.intValue();
        }
        if (Long.class.isAssignableFrom(clazz)) {
            return number.longValue();
        }
        if (Float.class.isAssignableFrom(clazz)) {
            return number.floatValue();
        }
        if (Double.class.isAssignableFrom(clazz)) {
            return number.doubleValue();
        }
        throw new SearchException();
    }

}
