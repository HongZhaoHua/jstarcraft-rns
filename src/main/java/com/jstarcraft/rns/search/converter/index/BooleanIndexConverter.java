package com.jstarcraft.rns.search.converter.index;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.ClassUtility;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.converter.IndexConverter;
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 布尔转换器
 * 
 * @author Birdy
 *
 */
public class BooleanIndexConverter implements IndexConverter {

    @Override
    public Iterable<IndexableField> convert(Map<Class<?>, List<KeyValue<Field, IndexConverter>>> context, String path, Field field, SearchIndex annotation, String name, Type type, Object data) {
        Collection<IndexableField> fields = new LinkedList<>();
        name = path  + name;
        Class<?> clazz = TypeUtility.getRawType(type, null);
        clazz = ClassUtility.primitiveToWrapper(clazz);
        if (AtomicBoolean.class.isAssignableFrom(clazz)) {
            fields.add(new IntPoint(name, AtomicBoolean.class.cast(data).get() ? 1 : 0));
            return fields;
        }
        if (Boolean.class.isAssignableFrom(clazz)) {
            fields.add(new IntPoint(name, Boolean.class.cast(data) ? 1 : 0));
            return fields;
        }
        throw new SearchException();
    }

}
