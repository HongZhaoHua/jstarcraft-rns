package com.jstarcraft.rns.search.converter.sort;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchSort;
import com.jstarcraft.rns.search.converter.SearchContext;
import com.jstarcraft.rns.search.converter.SortConverter;
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 对象排序转换器
 * 
 * @author Birdy
 *
 */
public class ObjectSortConverter implements SortConverter {

    @Override
    public Iterable<IndexableField> convert(SearchContext context, String path, Field field, SearchSort annotation, Type type, Object data) {
        Collection<IndexableField> indexables = new LinkedList<>();
        Class<?> clazz = TypeUtility.getRawType(type, null);

        try {
            // TODO 此处需要代码重构
            for (KeyValue<Field, SortConverter> keyValue : context.getSortKeyValues(clazz)) {
                // TODO 此处代码可以优反射次数.
                field = keyValue.getKey();
                SortConverter converter = keyValue.getValue();
                annotation = field.getAnnotation(SearchSort.class);
                String name = field.getName();
                for (IndexableField indexable : converter.convert(context, path + "." + name, field, annotation, field.getGenericType(), field.get(data))) {
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
