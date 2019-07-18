package com.jstarcraft.rns.search.sort;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.apache.lucene.document.DoubleDocValuesField;
import org.apache.lucene.document.FloatDocValuesField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.ClassUtility;
import com.jstarcraft.rns.search.annotation.SearchSort;
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 数值转换器
 * 
 * @author Birdy
 *
 */
public class NumberConverter implements SortConverter {

    @Override
    public Iterable<IndexableField> convert(Map<Field, SortConverter> context, String path, Field field, SearchSort annotation, String name, Type type, Object data) {
        Class<?> clazz = TypeUtility.getRawType(type, null);
        clazz = ClassUtility.primitiveToWrapper(clazz);
        if (Byte.class == clazz) {

        }
        if (Short.class == clazz) {

        }
        if (Integer.class == clazz) {
            Collection<IndexableField> fields = new LinkedList<>();
            fields.add(new NumericDocValuesField(name, (Integer) data));
            return fields;
        }
        if (Long.class == clazz) {
            Collection<IndexableField> fields = new LinkedList<>();
            fields.add(new NumericDocValuesField(name, (Long) data));
            return fields;
        }
        if (Float.class == clazz) {
            Collection<IndexableField> fields = new LinkedList<>();
            fields.add(new FloatDocValuesField(name, (Float) data));
            return fields;
        }
        if (Double.class == clazz) {
            Collection<IndexableField> fields = new LinkedList<>();
            fields.add(new DoubleDocValuesField(name, (Double) data));
            return fields;
        }
        throw new SearchException();
    }

}
