/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations.combotools;

import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import org.bukkit.Material;

import java.util.Set;

public class ComboShovel extends AbstractItemFeatureImpl {
    protected static Set<Material> acceptedMaterials = Set.of(
        Material.DIRT,
        Material.GRASS,
        Material.SAND,
        Material.CLAY,
        Material.MYCELIUM,
        Material.FARMLAND,
        Material.GRAVEL,
        Material.SNOW,
        Material.SNOW_BLOCK,
        Material.SOUL_SAND
    );
}
