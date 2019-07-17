package com.jstarcraft.rns.search.converter;

import java.lang.reflect.Type;
import java.util.Collection;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.rns.search.annotation.SearchEncode;

/**
 * 对象转换器
 * 
 * @author Birdy
 *
 */
public class ObjectConverter implements SearchConverter {

    @Override
    public Collection<IndexableField> convert(String name, Type type, Object data, SearchEncode encode) {
        // TODO Auto-generated method stub
        return null;
    }

}
