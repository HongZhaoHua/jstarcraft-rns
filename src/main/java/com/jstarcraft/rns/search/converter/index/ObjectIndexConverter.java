package com.jstarcraft.rns.search.converter.index;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.converter.IndexConverter;
import com.jstarcraft.rns.search.converter.SearchContext;
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 对象索引转换器
 * 
 * @author Birdy
 *
 */
public class ObjectIndexConverter implements IndexConverter {

    @Override
    public Iterable<IndexableField> convert(SearchContext context, String path, Field field, SearchIndex annotation, Type type, Object instance) {
        Collection<IndexableField> indexables = new LinkedList<>();
        Class<?> clazz = TypeUtility.getRawType(type, null);

        try {
            // TODO 此处需要代码重构
            for (KeyValue<Field, IndexConverter> keyValue : context.getIndexKeyValues(clazz)) {
                // TODO 此处代码可以优反射次数.
                field = keyValue.getKey();
                IndexConverter converter = keyValue.getValue();
                annotation = field.getAnnotation(SearchIndex.class);
                String name = field.getName();
                type = field.getGenericType();
                Object data = field.get(instance);
                for (IndexableField indexable : converter.convert(context, path + "." + name, field, annotation, type, data)) {
                    indexables.add(indexable);
                }
            }
            return indexables;
        } catch (Exception exception) {
            // TODO
            throw new SearchException(exception);
        }
    }

}
