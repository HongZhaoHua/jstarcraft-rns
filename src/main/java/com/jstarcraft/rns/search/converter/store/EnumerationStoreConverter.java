package com.jstarcraft.rns.search.converter.store;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchStore;
import com.jstarcraft.rns.search.converter.StoreConverter;

/**
 * 枚举转换器
 * 
 * @author Birdy
 *
 */
public class EnumerationStoreConverter implements StoreConverter {

    @Override
    public NavigableMap<String, IndexableField> encode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, String name, Type type, Object data) {
        NavigableMap<String, IndexableField> indexables = new TreeMap<>();
        name = path + name;
        indexables.put(name, new StoredField(name, data.toString()));
        return indexables;
    }

    @Override
    public Object decode(Map<Class<?>, List<KeyValue<Field, StoreConverter>>> context, String path, Field field, SearchStore annotation, String name, Type type, NavigableMap<String, IndexableField> document) {
        String from = name;
        char character = name.charAt(name.length() - 1);
        character++;
        String to = name.substring(0, name.length() - 1) + character;
        document = document.subMap(from, true, to, false);
        IndexableField indexable = document.firstEntry().getValue();
        Class clazz = TypeUtility.getRawType(type, null);
        return Enum.valueOf(clazz, indexable.stringValue());
    }

}
