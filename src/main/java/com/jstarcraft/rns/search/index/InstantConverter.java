package com.jstarcraft.rns.search.index;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.exception.SearchException;
import com.jstarcraft.rns.search.sort.SortConverter;

/**
 * 时间转换器
 * 
 * @author Birdy
 *
 */
public class InstantConverter implements IndexConverter {

    @Override
    public Iterable<IndexableField> convert(Map<Field, SortConverter> context, String path, Field field, SearchIndex annotation, String name, Type type, Object data) {
        Class<?> clazz = TypeUtility.getRawType(type, null);
        if (Instant.class == clazz) {

        }
        if (Date.class == clazz) {

        }
        throw new SearchException();
    }

}
