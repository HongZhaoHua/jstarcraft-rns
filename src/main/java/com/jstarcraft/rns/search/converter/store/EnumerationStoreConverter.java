package com.jstarcraft.rns.search.converter.store;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.rns.search.annotation.SearchStore;
import com.jstarcraft.rns.search.converter.SearchContext;
import com.jstarcraft.rns.search.converter.StoreConverter;

/**
 * 枚举存储转换器
 * 
 * @author Birdy
 *
 */
public class EnumerationStoreConverter implements StoreConverter {

    @Override
    public Object decode(SearchContext context, String path, Field field, SearchStore annotation, Type type, NavigableMap<String, IndexableField> document) {
        String from = path;
        char character = path.charAt(path.length() - 1);
        character++;
        String to = path.substring(0, path.length() - 1) + character;
        document = document.subMap(from, true, to, false);
        IndexableField indexable = document.firstEntry().getValue();
        Class clazz = TypeUtility.getRawType(type, null);
        return Enum.valueOf(clazz, indexable.stringValue());
    }

    @Override
    public NavigableMap<String, IndexableField> encode(SearchContext context, String path, Field field, SearchStore annotation, Type type, Object data) {
        NavigableMap<String, IndexableField> indexables = new TreeMap<>();
        indexables.put(path, new StoredField(path, data.toString()));
        return indexables;
    }

}
