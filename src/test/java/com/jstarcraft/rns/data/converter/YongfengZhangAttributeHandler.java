package com.jstarcraft.rns.data.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map.Entry;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.attribute.QualityAttribute;
import com.jstarcraft.ai.data.attribute.QuantityAttribute;
import com.jstarcraft.ai.data.exception.DataException;
import com.jstarcraft.core.common.conversion.csv.ConversionUtility;
import com.jstarcraft.core.utility.KeyValue;

import it.unimi.dsi.fastutil.ints.Int2FloatRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2FloatSortedMap;
import it.unimi.dsi.fastutil.ints.Int2IntRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class YongfengZhangAttributeHandler extends DefaultHandler {

    private DataSpace space;

    private QualityAttribute<String> userAttribute;

    private QualityAttribute<String> itemAttribute;

    private QualityAttribute<String> wordAttribute;

    private QuantityAttribute<Float> scoreAttribute;

    private QuantityAttribute<Float> sentimentAttribute;

    private StringBuffer buffer = new StringBuffer();

    YongfengZhangAttributeHandler(DataSpace space) {
        this.space = space;
        this.userAttribute = space.getQualityAttribute("user");
        this.itemAttribute = space.getQualityAttribute("item");
        this.wordAttribute = space.getQualityAttribute("word");
        this.scoreAttribute = space.getQuantityAttribute("score");
        this.sentimentAttribute = space.getQuantityAttribute("sentiment");
    }

    private void parseData(BufferedReader buffer) throws IOException {
        String line = buffer.readLine();
        line = buffer.readLine();
        String[] strings = line.split("\t");
        String user = strings[0];
        userAttribute.convertData(user);
        String item = strings[1];
        itemAttribute.convertData(item);
        Float score = Float.valueOf(strings[3]);
        scoreAttribute.convertData(score);

        line = buffer.readLine();
        line = buffer.readLine();
        strings = line.split("\t");
        for (String string : strings) {
            string = string.substring(1, string.length() - 1);
            String[] elements = string.split(", ");
            String word = elements[0];
            wordAttribute.convertData(word);
            Float sentiment = elements[4].equalsIgnoreCase("N") ? Float.valueOf(elements[2]) : -Float.valueOf(elements[2]);
            sentiment *= Float.valueOf(elements[3]);
            sentimentAttribute.convertData(sentiment);
        }
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
    }

    @Override
    public void characters(char characters[], int index, int length) throws SAXException {
        buffer.append(characters, index, length);
    }

}