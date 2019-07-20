package com.jstarcraft.rns.search.converter;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import com.jstarcraft.core.codec.specification.ClassDefinition;
import com.jstarcraft.core.codec.specification.CodecDefinition;
import com.jstarcraft.core.common.reflection.ReflectionUtility;
import com.jstarcraft.core.common.reflection.Specification;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.annotation.SearchSort;
import com.jstarcraft.rns.search.annotation.SearchStore;
import com.jstarcraft.rns.search.converter.index.ArrayIndexConverter;
import com.jstarcraft.rns.search.converter.index.BooleanIndexConverter;
import com.jstarcraft.rns.search.converter.index.CollectionIndexConverter;
import com.jstarcraft.rns.search.converter.index.EnumerationIndexConverter;
import com.jstarcraft.rns.search.converter.index.InstantIndexConverter;
import com.jstarcraft.rns.search.converter.index.MapIndexConverter;
import com.jstarcraft.rns.search.converter.index.NumberIndexConverter;
import com.jstarcraft.rns.search.converter.index.ObjectIndexConverter;
import com.jstarcraft.rns.search.converter.index.StringIndexConverter;
import com.jstarcraft.rns.search.converter.sort.ArraySortConverter;
import com.jstarcraft.rns.search.converter.sort.BooleanSortConverter;
import com.jstarcraft.rns.search.converter.sort.CollectionSortConverter;
import com.jstarcraft.rns.search.converter.sort.EnumerationSortConverter;
import com.jstarcraft.rns.search.converter.sort.InstantSortConverter;
import com.jstarcraft.rns.search.converter.sort.MapSortConverter;
import com.jstarcraft.rns.search.converter.sort.NumberSortConverter;
import com.jstarcraft.rns.search.converter.sort.ObjectSortConverter;
import com.jstarcraft.rns.search.converter.sort.StringSortConverter;
import com.jstarcraft.rns.search.converter.store.ArrayStoreConverter;
import com.jstarcraft.rns.search.converter.store.BooleanStoreConverter;
import com.jstarcraft.rns.search.converter.store.CollectionStoreConverter;
import com.jstarcraft.rns.search.converter.store.EnumerationStoreConverter;
import com.jstarcraft.rns.search.converter.store.InstantStoreConverter;
import com.jstarcraft.rns.search.converter.store.MapStoreConverter;
import com.jstarcraft.rns.search.converter.store.NumberStoreConverter;
import com.jstarcraft.rns.search.converter.store.ObjectStoreConverter;
import com.jstarcraft.rns.search.converter.store.StringStoreConverter;
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

    public static final EnumMap<Specification, IndexConverter> INDEX_CONVERTERS = new EnumMap<>(Specification.class);

    public static final EnumMap<Specification, SortConverter> SORT_CONVERTERS = new EnumMap<>(Specification.class);

    public static final EnumMap<Specification, StoreConverter> STORE_CONVERTERS = new EnumMap<>(Specification.class);

    static {
        INDEX_CONVERTERS.put(Specification.ARRAY, new ArrayIndexConverter());
        INDEX_CONVERTERS.put(Specification.BOOLEAN, new BooleanIndexConverter());
        INDEX_CONVERTERS.put(Specification.COLLECTION, new CollectionIndexConverter());
        INDEX_CONVERTERS.put(Specification.ENUMERATION, new EnumerationIndexConverter());
        INDEX_CONVERTERS.put(Specification.INSTANT, new InstantIndexConverter());
        INDEX_CONVERTERS.put(Specification.MAP, new MapIndexConverter());
        INDEX_CONVERTERS.put(Specification.NUMBER, new NumberIndexConverter());
        INDEX_CONVERTERS.put(Specification.OBJECT, new ObjectIndexConverter());
        INDEX_CONVERTERS.put(Specification.STRING, new StringIndexConverter());
    }

    static {
        SORT_CONVERTERS.put(Specification.ARRAY, new ArraySortConverter());
        SORT_CONVERTERS.put(Specification.BOOLEAN, new BooleanSortConverter());
        SORT_CONVERTERS.put(Specification.COLLECTION, new CollectionSortConverter());
        SORT_CONVERTERS.put(Specification.ENUMERATION, new EnumerationSortConverter());
        SORT_CONVERTERS.put(Specification.INSTANT, new InstantSortConverter());
        SORT_CONVERTERS.put(Specification.MAP, new MapSortConverter());
        SORT_CONVERTERS.put(Specification.NUMBER, new NumberSortConverter());
        SORT_CONVERTERS.put(Specification.OBJECT, new ObjectSortConverter());
        SORT_CONVERTERS.put(Specification.STRING, new StringSortConverter());
    }

    static {
        STORE_CONVERTERS.put(Specification.ARRAY, new ArrayStoreConverter());
        STORE_CONVERTERS.put(Specification.BOOLEAN, new BooleanStoreConverter());
        STORE_CONVERTERS.put(Specification.COLLECTION, new CollectionStoreConverter());
        STORE_CONVERTERS.put(Specification.ENUMERATION, new EnumerationStoreConverter());
        STORE_CONVERTERS.put(Specification.INSTANT, new InstantStoreConverter());
        STORE_CONVERTERS.put(Specification.MAP, new MapStoreConverter());
        STORE_CONVERTERS.put(Specification.NUMBER, new NumberStoreConverter());
        STORE_CONVERTERS.put(Specification.OBJECT, new ObjectStoreConverter());
        STORE_CONVERTERS.put(Specification.STRING, new StringStoreConverter());
    }

    private ClassDefinition saveDefinition;

    private ClassDefinition loadDefinition;

    private Map<Class<?>, List<KeyValue<Field, IndexConverter>>> indexKeyValues;

    private Map<Class<?>, List<KeyValue<Field, SortConverter>>> sortKeyValues;

    private Map<Class<?>, List<KeyValue<Field, StoreConverter>>> storeKeyValues;

    private void parse(ClassDefinition definition) {
        List<KeyValue<Field, IndexConverter>> indexKeyValues = new LinkedList<>();
        List<KeyValue<Field, SortConverter>> sortKeyValues = new LinkedList<>();
        List<KeyValue<Field, StoreConverter>> storeKeyValues = new LinkedList<>();

        ReflectionUtility.doWithFields(definition.getType(), (field) -> {
            ReflectionUtility.makeAccessible(field);
            Type type = field.getGenericType();
            Specification specification = Specification.getSpecification(type);

            try {
                SearchIndex index = field.getAnnotation(SearchIndex.class);
                if (index != null) {
                    Class<? extends IndexConverter> clazz = index.clazz();
                    if (IndexConverter.class == clazz) {
                        IndexConverter converter = INDEX_CONVERTERS.get(specification);
                        indexKeyValues.add(new KeyValue<>(field, converter));
                    } else {
                        IndexConverter converter = clazz.newInstance();
                        indexKeyValues.add(new KeyValue<>(field, converter));
                    }
                }

                SearchSort sort = field.getAnnotation(SearchSort.class);
                if (sort != null) {
                    Class<? extends SortConverter> clazz = sort.clazz();
                    if (SortConverter.class == clazz) {
                        SortConverter converter = SORT_CONVERTERS.get(specification);
                        sortKeyValues.add(new KeyValue<>(field, converter));
                    } else {
                        SortConverter converter = clazz.newInstance();
                        sortKeyValues.add(new KeyValue<>(field, converter));
                    }
                }

                SearchStore store = field.getAnnotation(SearchStore.class);
                if (store != null) {
                    Class<? extends StoreConverter> clazz = store.clazz();
                    if (StoreConverter.class == clazz) {
                        StoreConverter converter = STORE_CONVERTERS.get(specification);
                        storeKeyValues.add(new KeyValue<>(field, converter));
                    } else {
                        StoreConverter converter = clazz.newInstance();
                        storeKeyValues.add(new KeyValue<>(field, converter));
                    }
                }
            } catch (Exception exception) {
                throw new SearchException(exception);
            }
        });

        this.indexKeyValues.put(definition.getType(), new ArrayList<>(indexKeyValues));
        this.sortKeyValues.put(definition.getType(), new ArrayList<>(sortKeyValues));
        this.storeKeyValues.put(definition.getType(), new ArrayList<>(storeKeyValues));
    }

    public SearchCodec(Class<S> saveClass, Class<L> loadClass) {
        CodecDefinition saveDefinition = CodecDefinition.instanceOf(saveClass);
        CodecDefinition loadDefinition = CodecDefinition.instanceOf(loadClass);
        this.indexKeyValues = new HashMap<>();
        this.sortKeyValues = new HashMap<>();
        this.storeKeyValues = new HashMap<>();

        for (ClassDefinition classDefinition : saveDefinition.getClassDefinitions()) {
            // 预定义的规范类型不需要分析
            if (Specification.type2Specifitions.containsKey(classDefinition.getType())) {
                continue;
            }
            parse(classDefinition);
        }
        for (ClassDefinition classDefinition : loadDefinition.getClassDefinitions()) {
            // 预定义的规范类型不需要分析
            if (Specification.type2Specifitions.containsKey(classDefinition.getType())) {
                continue;
            }
            parse(classDefinition);
        }

        this.saveDefinition = saveDefinition.getClassDefinition(saveClass);
        this.loadDefinition = loadDefinition.getClassDefinition(loadClass);
    }

    public List<KeyValue<Field, IndexConverter>> getIndexKeyValues(Class<?> clazz) {
        return this.indexKeyValues.get(clazz);
    }

    public List<KeyValue<Field, SortConverter>> getSortKeyValues(Class<?> clazz) {
        return this.sortKeyValues.get(clazz);
    }

    public List<KeyValue<Field, StoreConverter>> getStoreKeyValues(Class<?> clazz) {
        return this.storeKeyValues.get(clazz);
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
            for (KeyValue<Field, IndexConverter> keyValue : this.indexKeyValues.get(this.saveDefinition.getType())) {
                // TODO 此处代码可以优反射次数.
                Field field = keyValue.getKey();
                IndexConverter converter = keyValue.getValue();
                SearchIndex annotation = field.getAnnotation(SearchIndex.class);
                String path = field.getName();
                Type type = field.getGenericType();
                Object data = field.get(instance);
                for (IndexableField indexable : converter.convert(this.indexKeyValues, path, field, annotation, type, data)) {
                    document.add(indexable);
                }
            }
            for (KeyValue<Field, SortConverter> keyValue : this.sortKeyValues.get(this.saveDefinition.getType())) {
                // TODO 此处代码可以优反射次数.
                Field field = keyValue.getKey();
                SortConverter converter = keyValue.getValue();
                SearchSort annotation = field.getAnnotation(SearchSort.class);
                String path = field.getName();
                Type type = field.getGenericType();
                Object data = field.get(instance);
                for (IndexableField indexable : converter.convert(this.sortKeyValues, path, field, annotation, type, data)) {
                    document.add(indexable);
                }
            }
            for (KeyValue<Field, StoreConverter> keyValue : this.storeKeyValues.get(this.saveDefinition.getType())) {
                // TODO 此处代码可以优反射次数.
                Field field = keyValue.getKey();
                StoreConverter converter = keyValue.getValue();
                SearchStore annotation = field.getAnnotation(SearchStore.class);
                String path = field.getName();
                Type type = field.getGenericType();
                Object data = field.get(instance);
                for (IndexableField indexable : converter.encode(this.storeKeyValues, path, field, annotation, type, data).values()) {
                    document.add(indexable);
                }
            }
            return document;
        } catch (Exception exception) {
            // TODO
            throw new SearchException(exception);
        }
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
            for (KeyValue<Field, StoreConverter> keyValue : this.storeKeyValues.get(this.loadDefinition.getType())) {
                // TODO 此处代码可以优反射次数.
                Field field = keyValue.getKey();
                StoreConverter converter = keyValue.getValue();
                SearchStore annotation = field.getAnnotation(SearchStore.class);
                String path = field.getName();
                Type type = field.getGenericType();
                Object data = converter.decode(this.storeKeyValues, path, field, annotation, type, indexables);
                field.set(instance, data);
            }
            return instance;
        } catch (Exception exception) {
            // TODO
            throw new SearchException(exception);
        }
    }

}
