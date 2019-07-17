package com.jstarcraft.rns.search;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.common.reflection.ReflectionUtility;
import com.jstarcraft.core.common.reflection.Specification;
import com.jstarcraft.rns.search.annotation.SearchAnalyze;
import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.annotation.SearchSort;
import com.jstarcraft.rns.search.annotation.SearchStore;
import com.jstarcraft.rns.search.converter.ArrayConverter;
import com.jstarcraft.rns.search.converter.BooleanConverter;
import com.jstarcraft.rns.search.converter.CollectionConverter;
import com.jstarcraft.rns.search.converter.EnumerationConverter;
import com.jstarcraft.rns.search.converter.InstantConverter;
import com.jstarcraft.rns.search.converter.MapConverter;
import com.jstarcraft.rns.search.converter.NumberConverter;
import com.jstarcraft.rns.search.converter.ObjectConverter;
import com.jstarcraft.rns.search.converter.SearchConverter;
import com.jstarcraft.rns.search.converter.StringConverter;
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

    protected static final EnumMap<Specification, SearchConverter> CONVERTERS = new EnumMap<>(Specification.class);

    static {
        CONVERTERS.put(Specification.ARRAY, new ArrayConverter());
        CONVERTERS.put(Specification.BOOLEAN, new BooleanConverter());
        CONVERTERS.put(Specification.COLLECTION, new CollectionConverter());
        CONVERTERS.put(Specification.ENUMERATION, new EnumerationConverter());
        CONVERTERS.put(Specification.INSTANT, new InstantConverter());
        CONVERTERS.put(Specification.MAP, new MapConverter());
        CONVERTERS.put(Specification.NUMBER, new NumberConverter());
        CONVERTERS.put(Specification.OBJECT, new ObjectConverter());
        CONVERTERS.put(Specification.STRING, new StringConverter());
    }

    private Map<Field, SearchConverter> converters;

    public SearchCodec(Class<S> saveClazz, Class<L> loadClazz) {
        ReflectionUtility.doWithFields(saveClazz, (field) -> {
            // TODO 检查是否存在自定义转换器

            Type type = field.getGenericType();
            Specification specification = Specification.getSpecification(type);
            SearchConverter converter = CONVERTERS.get(specification);
            if (converter == null) {
                throw new SearchException();
            } else {
                converters.put(field, converter);
            }
        });
    }

    /**
     * 编码
     * 
     * @param object
     * @return
     */
    public Document encode(S object) {
        Document document = new Document();
        try {
            for (Entry<Field, SearchConverter> term : converters.entrySet()) {
                Field field = term.getKey();

                // TODO 此处可以考虑优化
                String name = field.getName();
                Type type = field.getGenericType();
                Object data = field.get(object);
                SearchAnalyze analyze = field.getAnnotation(SearchAnalyze.class);
                SearchIndex index = field.getAnnotation(SearchIndex.class);
                SearchSort sort = field.getAnnotation(SearchSort.class);
                SearchStore store = field.getAnnotation(SearchStore.class);

                SearchConverter converter = term.getValue();
                for (IndexableField indexable : converter.convert(name, type, data, analyze, index, sort, store)) {
                    document.add(indexable);
                }
            }
        } catch (Exception exception) {
            // TODO
        }
        return document;
    }

    /**
     * 解码
     * 
     * @param document
     * @return
     */
    public L decode(Document document) {
        return null;
    }

}
