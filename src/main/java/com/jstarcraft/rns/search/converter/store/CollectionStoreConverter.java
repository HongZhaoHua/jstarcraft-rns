package com.jstarcraft.rns.search.converter.store;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.apache.commons.csv.CSVPrinter;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.codec.csv.converter.CsvConverter;
import com.jstarcraft.core.codec.specification.ClassDefinition;
import com.jstarcraft.core.common.reflection.Specification;
import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.core.utility.StringUtility;
import com.jstarcraft.rns.search.annotation.SearchStore;
import com.jstarcraft.rns.search.converter.StoreConverter;

/**
 * 集合存储转换器
 * 
 * @author Birdy
 *
 */
public class CollectionStoreConverter implements StoreConverter {

    @Override
    public Object decode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, Type type, NavigableMap<String, IndexableField> document) {
//        // TODO 处理null
//        Iterator<String> in = context.getInputStream();
//        String check = in.next();
//        if (StringUtility.isEmpty(check)) {
//            return null;
//        }
//        int length = Integer.valueOf(check);
//        Class<?> clazz = TypeUtility.getRawType(type, null);
//        // 兼容UniMi
//        type = TypeUtility.refineType(type, Collection.class);
//        ParameterizedType parameterizedType = ParameterizedType.class.cast(type);
//        Type[] types = parameterizedType.getActualTypeArguments();
//        ClassDefinition definition = context.getClassDefinition(clazz);
//        Collection<Object> collection = (Collection) definition.getInstance();
//        Class<?> elementClazz = TypeUtility.getRawType(types[0], null);
//        CsvConverter converter = context.getCsvConverter(Specification.getSpecification(elementClazz));
//        for (int index = 0; index < length; index++) {
//            Object element = converter.readValueFrom(context, types[0]);
//            collection.add(element);
//        }
//        return collection;
        return null;
    }

    @Override
    public NavigableMap<String, IndexableField> encode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, Type type, Object data) {
//        // TODO 处理null
//        CSVPrinter out = context.getOutputStream();
//        if (value == null) {
//            out.print(StringUtility.EMPTY);
//            return;
//        }
//        // 兼容UniMi
//        type = TypeUtility.refineType(type, Collection.class);
//        ParameterizedType parameterizedType = ParameterizedType.class.cast(type);
//        Type[] types = parameterizedType.getActualTypeArguments();
//        Collection<?> collection = Collection.class.cast(value);
//        out.print(collection.size());
//        Class<?> elementClazz = TypeUtility.getRawType(types[0], null);
//        CsvConverter converter = context.getCsvConverter(Specification.getSpecification(elementClazz));
//        for (Object element : collection) {
//            converter.writeValueTo(context, types[0], element);
//        }
//        return;
        return null;
    }

}
