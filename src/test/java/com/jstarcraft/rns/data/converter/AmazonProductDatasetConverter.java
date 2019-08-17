package com.jstarcraft.rns.data.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.attribute.QualityAttribute;
import com.jstarcraft.ai.data.attribute.QuantityAttribute;
import com.jstarcraft.ai.data.converter.StreamConverter;
import com.jstarcraft.core.common.conversion.csv.ConversionUtility;
import com.jstarcraft.core.common.conversion.json.JsonUtility;
import com.jstarcraft.core.common.reflection.TypeUtility;
import com.jstarcraft.core.utility.KeyValue;

import it.unimi.dsi.fastutil.ints.Int2FloatRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2FloatSortedMap;
import it.unimi.dsi.fastutil.ints.Int2IntRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class AmazonProductDatasetConverter extends StreamConverter {

    private final static Type jsonType = TypeUtility.parameterize(HashMap.class, String.class, Object.class);

    private QualityAttribute<String> wordAttribute;

    public AmazonProductDatasetConverter(QualityAttribute<String> wordAttribute, Collection<QualityAttribute> qualityAttributes, Collection<QuantityAttribute> quantityAttributes) {
        super(qualityAttributes, quantityAttributes);
        this.wordAttribute = wordAttribute;
    }

    @Override
    protected int parseData(DataModule module, BufferedReader buffer) throws IOException {
        Int2IntSortedMap qualityFeatures = new Int2IntRBTreeMap();
        Int2FloatSortedMap quantityFeatures = new Int2FloatRBTreeMap();
        Int2ObjectOpenHashMap<Object> datas = new Int2ObjectOpenHashMap<>();
        int count = 0;
        while (true) {
            datas.clear();
            String line = buffer.readLine();
            if (line == null) {
                break;
            }
            HashMap<String, Object> json = JsonUtility.string2Object(line, jsonType);
            String user = (String) json.get("reviewerID");
            datas.put(0, user);
            String item = (String) json.get("asin");
            datas.put(1, item);
            Number score = (Number) json.get("overall");
            datas.put(2, score);

            String[] words = ((String) json.get("summary")).split(" ");
            for (String word : words) {
                int wordIndex = wordAttribute.convertData(word);
                datas.put(3 + wordIndex, Float.valueOf(1F));
            }

            for (Int2ObjectMap.Entry<Object> data : datas.int2ObjectEntrySet()) {
                int index = data.getIntKey();
                Object value = data.getValue();
                Entry<Integer, KeyValue<String, Boolean>> term = module.getOuterKeyValue(index);
                KeyValue<String, Boolean> keyValue = term.getValue();
                if (keyValue.getValue()) {
                    QualityAttribute attribute = qualityAttributes.get(keyValue.getKey());
                    value = ConversionUtility.convert(value, attribute.getType());
                    int feature = attribute.convertData((Comparable) value);
                    qualityFeatures.put(module.getQualityInner(keyValue.getKey()) + index - term.getKey(), feature);
                } else {
                    QuantityAttribute attribute = quantityAttributes.get(keyValue.getKey());
                    value = ConversionUtility.convert(value, attribute.getType());
                    float feature = attribute.convertData((Number) value);
                    quantityFeatures.put(module.getQuantityInner(keyValue.getKey()) + index - term.getKey(), feature);
                }
            }
            module.associateInstance(qualityFeatures, quantityFeatures);
            qualityFeatures.clear();
            quantityFeatures.clear();
            count++;
        }
        return count;
    }

}
