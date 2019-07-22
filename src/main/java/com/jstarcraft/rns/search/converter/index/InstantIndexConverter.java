package com.jstarcraft.rns.search.converter.index;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.converter.IndexConverter;
import com.jstarcraft.rns.search.converter.SearchContext;
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 时间索引转换器
 * 
 * @author Birdy
 *
 */
public class InstantIndexConverter implements IndexConverter {

    @Override
    public Iterable<IndexableField> convert(SearchContext context, String path, Field field, SearchIndex annotation, Type type, Object data) {
        Collection<IndexableField> fields = new LinkedList<>();
        Class<?> clazz = TypeUtility.getRawType(type, null);
        if (Instant.class.isAssignableFrom(clazz)) {
            Instant instant = (Instant) data;
            fields.add(new LongPoint(path, instant.toEpochMilli()));
            return fields;
        }
        if (Date.class.isAssignableFrom(clazz)) {
            Date instant = (Date) data;
            fields.add(new LongPoint(path, instant.getTime()));
            return fields;
        }
        if (LocalDate.class.isAssignableFrom(clazz)) {

        }
        if (LocalTime.class.isAssignableFrom(clazz)) {

        }
        if (LocalDateTime.class.isAssignableFrom(clazz)) {

        }
        if (ZonedDateTime.class.isAssignableFrom(clazz)) {

        }
        if (ZoneOffset.class.isAssignableFrom(clazz)) {

        }
        throw new SearchException();
    }

}
