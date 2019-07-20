package com.jstarcraft.rns.search.converter.store;

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
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchStore;
import com.jstarcraft.rns.search.converter.StoreConverter;
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 时间转换器
 * 
 * @author Birdy
 *
 */
public class InstantStoreConverter implements StoreConverter {

    @Override
    public Iterable<IndexableField> encode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, String name, Type type, Object data) {
        Collection<IndexableField> fields = new LinkedList<>();
        name = path  + name;
        Class<?> clazz = TypeUtility.getRawType(type, null);
        if (Instant.class.isAssignableFrom(clazz)) {
            Instant instant = (Instant) data;
            fields.add(new StoredField(name, instant.toEpochMilli()));
            return fields;
        }
        if (Date.class.isAssignableFrom(clazz)) {
            Date instant = (Date) data;
            fields.add(new StoredField(name, instant.getTime()));
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

    @Override
    public Object decode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, String name, Type type, Iterable<IndexableField> document) {
        // TODO Auto-generated method stub
        return null;
    }

}
