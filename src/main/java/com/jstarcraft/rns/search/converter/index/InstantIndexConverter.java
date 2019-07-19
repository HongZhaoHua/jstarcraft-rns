package com.jstarcraft.rns.search.converter.index;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.converter.IndexConverter;
import com.jstarcraft.rns.search.converter.SortConverter;
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 时间转换器
 * 
 * @author Birdy
 *
 */
public class InstantIndexConverter implements IndexConverter {

    @Override
    public Iterable<IndexableField> convert(Map<Class<?>, List<KeyValue<Field, IndexConverter>>> context, String path, Field field, SearchIndex annotation, String name, Type type, Object data) {
        Class<?> clazz = TypeUtility.getRawType(type, null);
        if (Instant.class.isAssignableFrom(clazz)) {

        }
        if (Date.class.isAssignableFrom(clazz)) {

        }
        throw new SearchException();
    }

}
