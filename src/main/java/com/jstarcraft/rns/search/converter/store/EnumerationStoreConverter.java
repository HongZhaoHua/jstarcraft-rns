package com.jstarcraft.rns.search.converter.store;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchStore;
import com.jstarcraft.rns.search.converter.StoreConverter;

/**
 * 枚举转换器
 * 
 * @author Birdy
 *
 */
public class EnumerationStoreConverter implements StoreConverter {

    @Override
    public Iterable<IndexableField> encode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, String name, Type type, Object data) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object decode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, String name, Type type, Iterable<IndexableField> document) {
        // TODO Auto-generated method stub
        return null;
    }

}
