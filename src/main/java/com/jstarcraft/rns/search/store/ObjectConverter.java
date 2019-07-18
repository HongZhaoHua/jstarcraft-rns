package com.jstarcraft.rns.search.store;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.rns.search.annotation.SearchEncode;
import com.jstarcraft.rns.search.annotation.SearchStore;
import com.jstarcraft.rns.search.sort.SortConverter;

/**
 * 对象转换器
 * 
 * @author Birdy
 *
 */
public class ObjectConverter implements StoreConverter {

    @Override
    public Iterable<IndexableField> encode(Map<Field, SortConverter> context, String path, Field field, SearchStore annotation, String name, Type type, Object data) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object decode(Map<Field, SortConverter> context, String path, Field field, SearchStore annotation, String name, Type type, Iterable<IndexableField> document) {
        // TODO Auto-generated method stub
        return null;
    }

}
