package com.jstarcraft.rns.search.converter;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

public class SearchContext {

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

    private Map<Class<?>, ClassDefinition> classDefinitions;

    private Map<Class<?>, List<KeyValue<Field, IndexConverter>>> indexKeyValues;

    private Map<Class<?>, List<KeyValue<Field, SortConverter>>> sortKeyValues;

    private Map<Class<?>, List<KeyValue<Field, StoreConverter>>> storeKeyValues;

    private void parse(ClassDefinition definition) {
        this.classDefinitions.put(definition.getType(), definition);
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

    SearchContext(CodecDefinition... definitions) {
        this.classDefinitions = new HashMap<>();
        this.indexKeyValues = new HashMap<>();
        this.sortKeyValues = new HashMap<>();
        this.storeKeyValues = new HashMap<>();
        for (CodecDefinition codecDefinition : definitions) {
            for (ClassDefinition classDefinition : codecDefinition.getClassDefinitions()) {
                // 预定义的规范类型不需要分析
                if (Specification.type2Specifitions.containsKey(classDefinition.getType())) {
                    continue;
                }
                parse(classDefinition);
            }
        }
    }

    public <T> T getInstance(Class<T> clazz) throws Exception {
        return (T) classDefinitions.get(clazz).getInstance();
    }

    public IndexConverter getIndexConverter(Specification specification) {
        return INDEX_CONVERTERS.get(specification);
    }

    public SortConverter getSortConverter(Specification specification) {
        return SORT_CONVERTERS.get(specification);
    }

    public StoreConverter getStoreConverter(Specification specification) {
        return STORE_CONVERTERS.get(specification);
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

}
