package com.jstarcraft.rns.search.converter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.rns.search.annotation.SearchEncode;

/**
 * 集合转换器
 * 
 * @author Birdy
 *
 */
public class CollectionConverter implements SearchConverter {

    @Override
    public Collection<IndexableField> convert(String name, Type type, Object data, SearchEncode encode) {
        ParameterizedType parameterizedType = ParameterizedType.class.cast(type);
        Type[] types = parameterizedType.getActualTypeArguments();
        return null;
    }

}
