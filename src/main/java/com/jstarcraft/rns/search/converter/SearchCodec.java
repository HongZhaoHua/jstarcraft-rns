package com.jstarcraft.rns.search.converter;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.codec.specification.ClassDefinition;
import com.jstarcraft.core.codec.specification.CodecDefinition;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.annotation.SearchSort;
import com.jstarcraft.rns.search.annotation.SearchStore;
import com.jstarcraft.rns.search.exception.SearchException;

/**
 * 搜索编解码器
 * 
 * @author Birdy
 *
 * @param <T>
 */
// TODO 以后会整合到Searcher
public class SearchCodec<S, L> {

    private SearchContext context;

    private ClassDefinition saveDefinition;

    private ClassDefinition loadDefinition;

    public SearchCodec(Class<S> saveClass, Class<L> loadClass) {
        CodecDefinition saveDefinition = CodecDefinition.instanceOf(saveClass);
        CodecDefinition loadDefinition = CodecDefinition.instanceOf(loadClass);

        this.context = new SearchContext(saveDefinition, loadDefinition);

        this.saveDefinition = saveDefinition.getClassDefinition(saveClass);
        this.loadDefinition = loadDefinition.getClassDefinition(loadClass);
    }

    /**
     * 解码
     * 
     * @param document
     * @return
     */
    public L decode(Document document) {
        try {
            NavigableMap<String, IndexableField> indexables = new TreeMap<>();
            for (IndexableField indexable : document) {
                indexables.put(indexable.name(), indexable);
            }
            L instance = (L) loadDefinition.getInstance();
            for (KeyValue<Field, StoreConverter> keyValue : this.context.getStoreKeyValues(this.loadDefinition.getType())) {
                // TODO 此处代码可以优反射次数.
                Field field = keyValue.getKey();
                StoreConverter converter = keyValue.getValue();
                SearchStore annotation = field.getAnnotation(SearchStore.class);
                String path = field.getName();
                Type type = field.getGenericType();
                Object data = converter.decode(this.context, path, field, annotation, type, indexables);
                field.set(instance, data);
            }
            return instance;
        } catch (Exception exception) {
            // TODO
            throw new SearchException(exception);
        }
    }

    /**
     * 编码
     * 
     * @param instance
     * @return
     */
    public Document encode(S instance) {
        try {
            Document document = new Document();
            for (KeyValue<Field, IndexConverter> keyValue : this.context.getIndexKeyValues(this.saveDefinition.getType())) {
                // TODO 此处代码可以优反射次数.
                Field field = keyValue.getKey();
                IndexConverter converter = keyValue.getValue();
                SearchIndex annotation = field.getAnnotation(SearchIndex.class);
                String path = field.getName();
                Type type = field.getGenericType();
                Object data = field.get(instance);
                for (IndexableField indexable : converter.convert(this.context, path, field, annotation, type, data)) {
                    document.add(indexable);
                }
            }
            for (KeyValue<Field, SortConverter> keyValue : this.context.getSortKeyValues(this.saveDefinition.getType())) {
                // TODO 此处代码可以优反射次数.
                Field field = keyValue.getKey();
                SortConverter converter = keyValue.getValue();
                SearchSort annotation = field.getAnnotation(SearchSort.class);
                String path = field.getName();
                Type type = field.getGenericType();
                Object data = field.get(instance);
                for (IndexableField indexable : converter.convert(this.context, path, field, annotation, type, data)) {
                    document.add(indexable);
                }
            }
            for (KeyValue<Field, StoreConverter> keyValue : this.context.getStoreKeyValues(this.saveDefinition.getType())) {
                // TODO 此处代码可以优反射次数.
                Field field = keyValue.getKey();
                StoreConverter converter = keyValue.getValue();
                SearchStore annotation = field.getAnnotation(SearchStore.class);
                String path = field.getName();
                Type type = field.getGenericType();
                Object data = field.get(instance);
                for (IndexableField indexable : converter.encode(this.context, path, field, annotation, type, data).values()) {
                    document.add(indexable);
                }
            }
            return document;
        } catch (Exception exception) {
            // TODO
            throw new SearchException(exception);
        }
    }

}
