package com.jstarcraft.rns.search.converter.index;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.converter.IndexConverter;
import com.jstarcraft.rns.search.converter.SearchContext;

/**
 * 对象索引转换器
 * 
 * @author Birdy
 *
 */
public class ObjectIndexConverter implements IndexConverter {

    @Override
    public Iterable<IndexableField> convert(SearchContext context, String path, Field field, SearchIndex annotation, Type type, Object data) {
        Collection<IndexableField> fields = new LinkedList<>();
        // TODO Auto-generated method stub
        return null;
    }

}
