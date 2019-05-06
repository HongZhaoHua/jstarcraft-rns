package com.jstarcraft.recommendation.task;

import java.util.TreeMap;

public class ModuleConfigurer {

    private String name;

    private TreeMap<Integer, String> configuration;

    public String getName() {
        return name;
    }

    public TreeMap<Integer, String> getConfiguration() {
        return configuration;
    }

}
