package com.jstarcraft.rns.search.converter.index;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.converter.IndexConverter;

/**
 * 对象转换器
 * 
 * @author Birdy
 *
 */
public class ObjectIndexConverter implements IndexConverter {

    @Override
    public Iterable<IndexableField> convert(Map<Class<?>, List<KeyValue<Field, IndexConverter>>> context, String path, Field field, SearchIndex annotation, Type type, Object data) {
        Collection<IndexableField> fields = new LinkedList<>();
        // TODO Auto-generated method stub
        return null;
    }

}
