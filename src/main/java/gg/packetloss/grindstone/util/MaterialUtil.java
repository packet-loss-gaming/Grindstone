/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import org.apache.commons.lang.Validate;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class MaterialUtil {
    public static Set<Material> generateMaterialSet(Predicate<Material> test) {
        List<Material> newOreBlocks = new ArrayList<>();

        for (Material material : Material.values()) {
            if (material.isLegacy()) {
                continue;
            }

            if (test.test(material)) {
                newOreBlocks.add(material);
            }
        }

        Set<Material> results = Set.copyOf(newOreBlocks);
        Validate.isTrue(!results.isEmpty());
        return results;
    }

    public static Set<Material> generatePrefixMaterialSet(String postfix) {
        return generateMaterialSet((material) -> material.name().startsWith(postfix));
    }

    public static Set<Material> generatePostfixMaterialSet(String postfix) {
        return generateMaterialSet((material) -> material.name().endsWith(postfix));
    }
}
