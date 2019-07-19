package com.jstarcraft.rns.search.converter.sort;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.DoubleDocValuesField;
import org.apache.lucene.document.FloatDocValuesField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.ClassUtility;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.core.utility.StringUtility;
import com.jstarcraft.rns.search.annotation.SearchSort;
import com.jstarcraft.rns.search.converter.SortConverter;
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 数值转换器
 * 
 * @author Birdy
 *
 */
public class NumberSortConverter implements SortConverter {

    @Override
    public Iterable<IndexableField> convert(Map<Class<?>, List<KeyValue<Field, SortConverter>>> context, String path, Field field, SearchSort annotation, String name, Type type, Object data) {
        Collection<IndexableField> fields = new LinkedList<>();
        name = path + StringUtility.DOT + name;
        Class<?> clazz = TypeUtility.getRawType(type, null);
        clazz = ClassUtility.primitiveToWrapper(clazz);
        if (Byte.class.isAssignableFrom(clazz)) {
            fields.add(new NumericDocValuesField(name, (Byte) data));
            return fields;
        }
        if (Short.class.isAssignableFrom(clazz)) {
            fields.add(new NumericDocValuesField(name, (Short) data));
            return fields;
        }
        if (Integer.class.isAssignableFrom(clazz)) {
            fields.add(new NumericDocValuesField(name, (Integer) data));
            return fields;
        }
        if (Long.class.isAssignableFrom(clazz)) {
            fields.add(new NumericDocValuesField(name, (Long) data));
            return fields;
        }
        if (Float.class.isAssignableFrom(clazz)) {
            fields.add(new FloatDocValuesField(name, (Float) data));
            return fields;
        }
        if (Double.class.isAssignableFrom(clazz)) {
            fields.add(new DoubleDocValuesField(name, (Double) data));
            return fields;
        }
        throw new SearchException();
    }

}
