package com.jstarcraft.rns.search.converter.store;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.ClassUtility;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchStore;
import com.jstarcraft.rns.search.converter.StoreConverter;
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 数值转换器
 * 
 * @author Birdy
 *
 */
public class NumberStoreConverter implements StoreConverter {

    @Override
    public Iterable<IndexableField> encode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, String name, Type type, Object data) {
        Collection<IndexableField> fields = new LinkedList<>();
        name = path  + name;
        Class<?> clazz = TypeUtility.getRawType(type, null);
        clazz = ClassUtility.primitiveToWrapper(clazz);
        if (Byte.class.isAssignableFrom(clazz)) {
            fields.add(new StoredField(name, (Byte) data));
            return fields;
        }
        if (Short.class.isAssignableFrom(clazz)) {
            fields.add(new StoredField(name, (Short) data));
            return fields;
        }
        if (Integer.class.isAssignableFrom(clazz)) {
            fields.add(new StoredField(name, (Integer) data));
            return fields;
        }
        if (Long.class.isAssignableFrom(clazz)) {
            fields.add(new StoredField(name, (Long) data));
            return fields;
        }
        if (Float.class.isAssignableFrom(clazz)) {
            fields.add(new StoredField(name, (Float) data));
            return fields;
        }
        if (Double.class.isAssignableFrom(clazz)) {
            fields.add(new StoredField(name, (Double) data));
            return fields;
        }
        throw new SearchException();
    }

    @Override
    public Object decode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, String name, Type type, Iterable<IndexableField> document) {
        // TODO Auto-generated method stub
        return null;
    }

}
