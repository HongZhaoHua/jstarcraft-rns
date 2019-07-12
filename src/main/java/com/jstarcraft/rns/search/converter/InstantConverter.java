package com.jstarcraft.rns.search.converter;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.ClassUtility;
import com.jstarcraft.rns.search.annotation.RetrievalAnalyze;
import com.jstarcraft.rns.search.annotation.RetrievalIndex;
import com.jstarcraft.rns.search.annotation.RetrievalSort;
import com.jstarcraft.rns.search.annotation.RetrievalStore;
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 时间转换器
 * 
 * @author Birdy
 *
 */
public class InstantConverter implements RetrievalConverter {

    @Override
    public Collection<IndexableField> convert(String name, Type type, Object data, RetrievalAnalyze analyze, RetrievalIndex index, RetrievalSort sort, RetrievalStore store) {
        Class<?> clazz = TypeUtility.getRawType(type, null);
        clazz = ClassUtility.primitiveToWrapper(clazz);
        if (Short.class == clazz) {

        }
        if (Integer.class == clazz) {
            Collection<IndexableField> fields = new LinkedList<>();
            if (index != null) {
                fields.add(new IntPoint(name, (Integer) data));
            }
            if (sort != null) {
                fields.add(new NumericDocValuesField(name, (Integer) data));
            }
            if (store != null) {
                fields.add(new StoredField(name, (Integer) data));
            }
            return fields;
        }
        throw new SearchException();
    }

}
