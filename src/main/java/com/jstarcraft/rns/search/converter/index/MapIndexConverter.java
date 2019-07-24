package com.jstarcraft.rns.search.converter.index;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.Specification;
import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.converter.IndexConverter;
import com.jstarcraft.rns.search.converter.SearchContext;
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 映射索引转换器
 * 
 * @author Birdy
 *
 */
public class MapIndexConverter implements IndexConverter {

    @Override
    public Iterable<IndexableField> convert(SearchContext context, String path, Field field, SearchIndex annotation, Type type, Object data) {
        Collection<IndexableField> indexables = new LinkedList<>();
        // 兼容UniMi
        type = TypeUtility.refineType(type, Map.class);
        ParameterizedType parameterizedType = ParameterizedType.class.cast(type);
        Type[] types = parameterizedType.getActualTypeArguments();
        Type keyType = types[0];
        Class<?> keyClazz = TypeUtility.getRawType(keyType, null);
        Type valueType = types[1];
        Class<?> valueClazz = TypeUtility.getRawType(valueType, null);

        try {
            // TODO 此处需要代码重构
            Map<Object, Object> map = Map.class.cast(data);
            Specification keySpecification = Specification.getSpecification(keyClazz);
            IndexConverter keyConverter = context.getIndexConverter(keySpecification);
            Specification valueSpecification = Specification.getSpecification(valueClazz);
            IndexConverter valueConverter = context.getIndexConverter(valueSpecification);

            // 只索引Key,不索引Value
            return keyConverter.convert(context, path, field, annotation, keyType, map.keySet());
        } catch (Exception exception) {
            // TODO
            throw new SearchException(exception);
        }
    }

}
