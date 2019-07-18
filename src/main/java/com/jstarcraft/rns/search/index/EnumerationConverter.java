package com.jstarcraft.rns.search.index;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.utility.StringUtility;
import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.sort.SortConverter;

/**
 * 枚举转换器
 * 
 * @author Birdy
 *
 */
public class EnumerationConverter implements IndexConverter {

    @Override
    public Iterable<IndexableField> convert(Map<Field, SortConverter> context, String path, Field field, SearchIndex annotation, String name, Type type, Object data) {
        name = path + StringUtility.DOT + name;
        Collection<IndexableField> fields = new LinkedList<>();
        fields.add(new StringField(name, data.toString(), Store.NO));
        return fields;
    }

}
