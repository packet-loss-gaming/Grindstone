/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.helper;

import com.sk89q.util.yaml.YAMLNode;
import com.sk89q.util.yaml.YAMLProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class YAMLResponseList {

    private YAMLProcessor processor;

    public YAMLResponseList(YAMLProcessor processor) {
        this.processor = processor;
    }

    public List<YAMLResponse> obtainResponses() {
        List<YAMLResponse> responses = new ArrayList<>();
        try {
            processor.load();
            Map<String, YAMLNode> nodes = processor.getNodes("responses");
            for (Map.Entry<String, YAMLNode> entry : nodes.entrySet()) {
                YAMLNode node = entry.getValue();
                Pattern pattern = Pattern.compile(node.getString("regex"));
                List<String> response = node.getStringList("response", new ArrayList<>());
                responses.add(new YAMLResponse(pattern, response));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return responses;
    }
}
