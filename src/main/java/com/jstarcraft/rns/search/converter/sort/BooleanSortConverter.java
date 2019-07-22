package com.jstarcraft.rns.search.converter.sort;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.ClassUtility;
import com.jstarcraft.rns.search.annotation.SearchSort;
import com.jstarcraft.rns.search.converter.SearchContext;
import com.jstarcraft.rns.search.converter.SortConverter;
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 布尔排序转换器
 * 
 * @author Birdy
 *
 */
public class BooleanSortConverter implements SortConverter {

    @Override
    public Iterable<IndexableField> convert(SearchContext context, String path, Field field, SearchSort annotation, Type type, Object data) {
        Collection<IndexableField> fields = new LinkedList<>();
        Class<?> clazz = TypeUtility.getRawType(type, null);
        clazz = ClassUtility.primitiveToWrapper(clazz);
        if (AtomicBoolean.class.isAssignableFrom(clazz)) {
            fields.add(new NumericDocValuesField(path, AtomicBoolean.class.cast(data).get() ? 1L : 0L));
            return fields;
        }
        if (Boolean.class.isAssignableFrom(clazz)) {
            fields.add(new NumericDocValuesField(path, Boolean.class.cast(data) ? 1L : 0L));
            return fields;
        }
        throw new SearchException();
    }

}
