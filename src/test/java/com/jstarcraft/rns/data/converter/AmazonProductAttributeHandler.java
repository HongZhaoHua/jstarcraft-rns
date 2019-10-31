package com.jstarcraft.rns.data.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;

import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.attribute.QualityAttribute;
import com.jstarcraft.ai.data.attribute.QuantityAttribute;
import com.jstarcraft.core.common.conversion.json.JsonUtility;
import com.jstarcraft.core.common.reflection.TypeUtility;

public class AmazonProductAttributeHandler {

    private final static Type jsonType = TypeUtility.parameterize(HashMap.class, String.class, Object.class);

    private QualityAttribute<String> userAttribute;

    private QualityAttribute<String> itemAttribute;

    private QualityAttribute<String> wordAttribute;

    private QuantityAttribute<Float> scoreAttribute;

    AmazonProductAttributeHandler(DataSpace space) {
        this.userAttribute = space.getQualityAttribute("user");
        this.itemAttribute = space.getQualityAttribute("item");
        this.wordAttribute = space.getQualityAttribute("word");
        this.scoreAttribute = space.getQuantityAttribute("score");
    }

    public void parseData(BufferedReader buffer) throws IOException {
        while (true) {
            String line = buffer.readLine();
            if (line == null) {
                return;
            }
            HashMap<String, Object> json = JsonUtility.string2Object(line, jsonType);
            String user = (String) json.get("reviewerID");
            userAttribute.convertData(user);
            String item = (String) json.get("asin");
            itemAttribute.convertData(item);
            Number score = (Number) json.get("overall");
            scoreAttribute.convertData(score.floatValue());

            String[] words = ((String) json.get("summary")).split(" ");
            for (String word : words) {
                wordAttribute.convertData(word);
            }
        }
    }

}