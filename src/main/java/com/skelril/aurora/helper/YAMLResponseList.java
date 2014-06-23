/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.helper;

import com.sk89q.util.yaml.YAMLNode;
import com.sk89q.util.yaml.YAMLProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class YAMLResponseList {

    private YAMLProcessor processor;

    public YAMLResponseList(YAMLProcessor processor) {
        this.processor = processor;
    }

    public Map<String, Response> obtainResponses() {
        Map<String, Response> responses = new HashMap<>();
        try {
            processor.load();
            Map<String, YAMLNode> nodes = processor.getNodes("responses");
            for (Map.Entry<String, YAMLNode> entry : nodes.entrySet()) {
                YAMLNode node = entry.getValue();
                Pattern pattern = Pattern.compile(node.getString("regex"));
                List<String> response = node.getStringList("response", new ArrayList<>());
                responses.put(entry.getKey(), new Response(pattern, response));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return responses;
    }

    public void saveResponses(Map<String, Response> responses) {
        for (Map.Entry<String, Response> entry : responses.entrySet()) {
            Response response = entry.getValue();
            YAMLNode node = processor.addNode("responses." + entry.getKey());
            node.setProperty("regex", response.getPattern());
            node.setProperty("response", response.getResponse());
        }
        processor.save();
    }
}
