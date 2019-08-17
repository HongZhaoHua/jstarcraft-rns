package com.jstarcraft.rns.data.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map.Entry;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.attribute.QualityAttribute;
import com.jstarcraft.ai.data.attribute.QuantityAttribute;
import com.jstarcraft.ai.data.converter.AbstractConverter;
import com.jstarcraft.ai.data.exception.DataException;
import com.jstarcraft.core.common.conversion.csv.ConversionUtility;
import com.jstarcraft.core.utility.KeyValue;

import it.unimi.dsi.fastutil.ints.Int2FloatRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2FloatSortedMap;
import it.unimi.dsi.fastutil.ints.Int2IntRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class YongfengZhangDatasetConverter extends AbstractConverter<InputStream> {

    class YongfengZhangInstanceHandler extends DefaultHandler {

        private DataModule module;

        private Int2IntSortedMap qualityFeatures = new Int2IntRBTreeMap();

        private Int2FloatSortedMap quantityFeatures = new Int2FloatRBTreeMap();

        private Int2ObjectOpenHashMap<Object> datas = new Int2ObjectOpenHashMap<>();

        private StringBuffer buffer = new StringBuffer();

        private int count;

        YongfengZhangInstanceHandler(DataModule module) {
            this.module = module;
        }

        private void parseData(BufferedReader buffer) throws IOException {
            datas.clear();
            String line = buffer.readLine();
            line = buffer.readLine();
            String[] strings = line.split("\t");
            String user = strings[0];
            datas.put(0, user);
            String item = strings[1];
            datas.put(1, item);
            Float score = Float.valueOf(strings[3]);
            datas.put(2, score);
            line = buffer.readLine();
            line = buffer.readLine();
            strings = line.split("\t");
            for (String string : strings) {
                string = string.substring(1, string.length() - 1);
                String[] elements = string.split(", ");
                String word = elements[0];
                int wordIndex = wordAttribute.convertData(word);
                Float sentiment = elements[4].equalsIgnoreCase("N") ? Float.valueOf(elements[2]) : -Float.valueOf(elements[2]);
                sentiment *= Float.valueOf(elements[3]);
                datas.put(3 + wordIndex, sentiment);
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
        }

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            buffer.setLength(0);
        }

        @Override
        public void endElement(String uri, String localName, String name) throws SAXException {
            try {
                try (StringReader reader = new StringReader(buffer.toString()); BufferedReader stream = new BufferedReader(reader)) {
                    parseData(stream);
                }
            } catch (Exception exception) {
                // TODO 处理日志.
                throw new DataException(exception);
            }
            count++;
        }

        @Override
        public void characters(char characters[], int index, int length) throws SAXException {
            buffer.append(characters, index, length);
        }

        public int getCount() {
            return count;
        }

    }

    private QualityAttribute<String> wordAttribute;

    public YongfengZhangDatasetConverter(QualityAttribute<String> wordAttribute, Collection<QualityAttribute> qualityAttributes, Collection<QuantityAttribute> quantityAttributes) {
        super(qualityAttributes, quantityAttributes);
        this.wordAttribute = wordAttribute;
    }

    @Override
    public int convert(DataModule module, InputStream iterator) {
        try {
            InputSource xmlSource = new InputSource(iterator);
            SAXParserFactory saxFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = saxFactory.newSAXParser();
            XMLReader sheetParser = saxParser.getXMLReader();
            YongfengZhangInstanceHandler handler = new YongfengZhangInstanceHandler(module);
            sheetParser.setContentHandler(handler);
            sheetParser.parse(xmlSource);
            return handler.getCount();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

}
