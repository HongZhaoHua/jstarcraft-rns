package com.jstarcraft.rns.search.sort;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;

import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.ClassUtility;
import com.jstarcraft.rns.search.annotation.SearchEncode;
import com.jstarcraft.rns.search.annotation.SearchSort;
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 时间转换器
 * 
 * @author Birdy
 *
 */
public class InstantConverter implements SortConverter {

    @Override
    public Iterable<IndexableField> convert(Map<Field, SortConverter> context, String path, Field field, SearchSort annotation, String name, Type type, Object data) {
        Class<?> clazz = TypeUtility.getRawType(type, null);
        if (Instant.class == clazz) {

        }
        if (Date.class == clazz) {

        }
        throw new SearchException();
    }

}
