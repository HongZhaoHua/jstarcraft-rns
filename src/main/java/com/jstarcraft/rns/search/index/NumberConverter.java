package com.jstarcraft.rns.search.index;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.ClassUtility;
import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.exception.SearchException;
import com.jstarcraft.rns.search.sort.SortConverter;

/**
 * 数值转换器
 * 
 * @author Birdy
 *
 */
public class NumberConverter implements IndexConverter {

    @Override
    public Iterable<IndexableField> convert(Map<Field, SortConverter> context, String path, Field field, SearchIndex annotation, String name, Type type, Object data) {
        Class<?> clazz = TypeUtility.getRawType(type, null);
        clazz = ClassUtility.primitiveToWrapper(clazz);
        if (Byte.class == clazz) {

        }
        if (Short.class == clazz) {

        }
        if (Integer.class == clazz) {
            Collection<IndexableField> fields = new LinkedList<>();
            fields.add(new IntPoint(name, (Integer) data));
            return fields;
        }
        if (Long.class == clazz) {
            Collection<IndexableField> fields = new LinkedList<>();
            fields.add(new LongPoint(name, (Long) data));
            return fields;
        }
        if (Float.class == clazz) {
            Collection<IndexableField> fields = new LinkedList<>();
            fields.add(new FloatPoint(name, (Float) data));
            return fields;
        }
        if (Double.class == clazz) {
            Collection<IndexableField> fields = new LinkedList<>();
            fields.add(new DoublePoint(name, (Double) data));
            return fields;
        }
        throw new SearchException();
    }

}
