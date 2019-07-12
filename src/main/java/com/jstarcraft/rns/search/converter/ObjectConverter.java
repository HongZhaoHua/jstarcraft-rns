package com.jstarcraft.rns.search.converter;

import java.lang.reflect.Type;
import java.util.Collection;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.rns.search.annotation.RetrievalAnalyze;
import com.jstarcraft.rns.search.annotation.RetrievalIndex;
import com.jstarcraft.rns.search.annotation.RetrievalSort;
import com.jstarcraft.rns.search.annotation.RetrievalStore;

/**
 * 对象转换器
 * 
 * @author Birdy
 *
 */
public class ObjectConverter implements RetrievalConverter {

    @Override
    public Collection<IndexableField> convert(String name, Type type, Object data, RetrievalAnalyze analyze, RetrievalIndex index, RetrievalSort sort, RetrievalStore store) {
        // TODO Auto-generated method stub
        return null;
    }

}
