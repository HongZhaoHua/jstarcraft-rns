package com.jstarcraft.rns.search.converter.store;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;

import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchStore;
import com.jstarcraft.rns.search.converter.StoreConverter;

/**
 * 字符串转换器
 * 
 * @author Birdy
 *
 */
public class StringStoreConverter implements StoreConverter {

    @Override
    public Iterable<IndexableField> encode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, String name, Type type, Object data) {
        Collection<IndexableField> fields = new LinkedList<>();
        name = path  + name;
        fields.add(new StoredField(name, new BytesRef(data.toString())));
        return fields;
    }

    @Override
    public Object decode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, String name, Type type, Iterable<IndexableField> document) {
        // TODO Auto-generated method stub
        return null;
    }

}
