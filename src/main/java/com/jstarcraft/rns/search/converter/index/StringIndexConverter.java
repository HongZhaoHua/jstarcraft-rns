package com.jstarcraft.rns.search.converter.index;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.annotation.SearchTerm;
import com.jstarcraft.rns.search.converter.IndexConverter;

/**
 * 字符串转换器
 * 
 * @author Birdy
 *
 */
public class StringIndexConverter implements IndexConverter {

    @Override
    public Iterable<IndexableField> convert(Map<Class<?>, List<KeyValue<Field, IndexConverter>>> context, String path, Field field, SearchIndex annotation, Type type, Object data) {
        Collection<IndexableField> fields = new LinkedList<>();
        FieldType configuration = new FieldType();
        configuration.setIndexOptions(IndexOptions.DOCS);
        if (annotation.analyze()) {
            configuration.setTokenized(true);

            SearchTerm negative = annotation.negative();
            if (negative.offset()) {
                configuration.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
            } else if (negative.position()) {
                configuration.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
            } else if (negative.frequency()) {
                configuration.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
            }

            SearchTerm positive = annotation.positive();
            if (positive.offset()) {
                configuration.setStoreTermVectorOffsets(true);
            }
            if (positive.position()) {
                configuration.setStoreTermVectorPositions(true);
            }
            if (positive.frequency()) {
                configuration.setStoreTermVectors(true);
            }
        }
        fields.add(new org.apache.lucene.document.Field(path, (String) data, configuration));
        return fields;
    }

}
