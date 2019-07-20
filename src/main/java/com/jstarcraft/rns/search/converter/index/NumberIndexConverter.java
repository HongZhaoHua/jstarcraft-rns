package com.jstarcraft.rns.search.converter.index;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.ClassUtility;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.converter.IndexConverter;
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 数值转换器
 * 
 * @author Birdy
 *
 */
public class NumberIndexConverter implements IndexConverter {

    @Override
    public Iterable<IndexableField> convert(Map<Class<?>, List<KeyValue<Field, IndexConverter>>> context, String path, Field field, SearchIndex annotation, Type type, Object data) {
        Collection<IndexableField> fields = new LinkedList<>();
        Class<?> clazz = TypeUtility.getRawType(type, null);
        clazz = ClassUtility.primitiveToWrapper(clazz);
        if (Byte.class.isAssignableFrom(clazz)) {
            fields.add(new IntPoint(path, (Byte) data));
            return fields;
        }
        if (Short.class.isAssignableFrom(clazz)) {
            fields.add(new IntPoint(path, (Short) data));
            return fields;
        }
        if (Integer.class.isAssignableFrom(clazz)) {
            fields.add(new IntPoint(path, (Integer) data));
            return fields;
        }
        if (Long.class.isAssignableFrom(clazz)) {
            fields.add(new LongPoint(path, (Long) data));
            return fields;
        }
        if (Float.class.isAssignableFrom(clazz)) {
            fields.add(new FloatPoint(path, (Float) data));
            return fields;
        }
        if (Double.class.isAssignableFrom(clazz)) {
            fields.add(new DoublePoint(path, (Double) data));
            return fields;
        }
        throw new SearchException();
    }

}
