package com.jstarcraft.rns.search.converter;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.rns.search.annotation.SearchEncode;
import com.jstarcraft.rns.search.annotation.SearchTerm;

/**
 * 枚举转换器
 * 
 * @author Birdy
 *
 */
public class EnumerationConverter implements SearchConverter {

    @Override
    public Collection<IndexableField> convert(String name, Type type, Object data, SearchEncode encode) {
        FieldType configuration = new FieldType();
        if (encode.index()) {
            configuration.setIndexOptions(IndexOptions.DOCS);
        }

        if (encode.analyze()) {
            configuration.setTokenized(true);

            if (encode.index()) {
                SearchTerm negative = encode.negative();
                if (negative.offset()) {
                    configuration.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
                } else if (negative.position()) {
                    configuration.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
                } else if (negative.frequency()) {
                    configuration.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
                }
            }

            SearchTerm positive = encode.positive();
            if (positive.offset()) {
                configuration.setStoreTermVectorOffsets(true);
            }
            if (positive.position()) {
                configuration.setStoreTermVectorPositions(true);
            }
            if (positive.frequency()) {
                configuration.setStoreTermVectors(true);
            }
        } else if (encode.sort()) {
            // 注意:分词字段存储docValue没有意义
            configuration.setDocValuesType(DocValuesType.SORTED);
        }

        if (encode.store()) {
            configuration.setStored(true);
        }

        Collection<IndexableField> fields = new LinkedList<>();
        fields.add(new org.apache.lucene.document.Field(name, (String) data, configuration));
        return fields;
    }

}
