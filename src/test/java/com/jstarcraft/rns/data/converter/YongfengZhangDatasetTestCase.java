package com.jstarcraft.rns.data.converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.attribute.QualityAttribute;
import com.jstarcraft.ai.data.converter.DataConverter;

/**
 * 演示如何自定义处理数据集
 * 
 * @author Birdy
 *
 */
public class YongfengZhangDatasetTestCase {

    @Test
    public void testDataset() throws Exception {
        File file = new File("data/labeled_DC/DC_feature_opinion/DC.txt");

        // 定义数据空间
        Map<String, Class<?>> qualityDifinitions = new HashMap<>();
        qualityDifinitions.put("user", String.class);
        qualityDifinitions.put("item", String.class);
        qualityDifinitions.put("word", String.class);
        Map<String, Class<?>> quantityDifinitions = new HashMap<>();
        quantityDifinitions.put("score", Float.class);
        quantityDifinitions.put("sentiment", Float.class);
        DataSpace dataSpace = new DataSpace(qualityDifinitions, quantityDifinitions);

        // 处理数据属性
        try (InputStream stream = new FileInputStream(file)) {
            InputSource xmlSource = new InputSource(stream);
            SAXParserFactory saxFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = saxFactory.newSAXParser();
            XMLReader sheetParser = saxParser.getXMLReader();
            YongfengZhangAttributeHandler handler = new YongfengZhangAttributeHandler(dataSpace);
            sheetParser.setContentHandler(handler);
            sheetParser.parse(xmlSource);
        }

        QualityAttribute<String> userAttribute = dataSpace.getQualityAttribute("user");
        QualityAttribute<String> itemAttribute = dataSpace.getQualityAttribute("item");
        QualityAttribute<String> wordAttribute = dataSpace.getQualityAttribute("word");
        Assert.assertEquals(89373, userAttribute.getSize());
        Assert.assertEquals(2397, itemAttribute.getSize());
        Assert.assertEquals(333, wordAttribute.getSize());

        // 定义数据模块
        // 使用word属性大小作为sentiment特征维度
        TreeMap<Integer, String> configuration = new TreeMap<>();
        configuration.put(1, "user");
        configuration.put(2, "item");
        configuration.put(3, "score");
        configuration.put(3 + wordAttribute.getSize(), "sentiment");
        DataModule dataModule = dataSpace.makeSparseModule("score", configuration, 1000000);

        // 处理数据实例
        DataConverter<InputStream> convertor = new YongfengZhangDatasetConverter(wordAttribute, dataSpace.getQualityAttributes(), dataSpace.getQuantityAttributes());
        try (InputStream stream = new FileInputStream(file)) {
            convertor.convert(dataModule, stream);
        }
        Assert.assertEquals(123732, dataModule.getSize());
    }

}
