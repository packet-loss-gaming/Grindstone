/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations.combotools;

import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import org.bukkit.Material;

import java.util.Set;

public class ComboAxe extends AbstractItemFeatureImpl {
    protected static Set<Material> acceptedMaterials = Set.of(
            Material.LOG,
            Material.LOG_2,
            Material.WOOD,
            Material.WOOD_STEP,
            Material.WOOD_DOUBLE_STEP,
            Material.WOOD_STAIRS,
            Material.DARK_OAK_STAIRS,
            Material.BIRCH_WOOD_STAIRS,
            Material.SPRUCE_WOOD_STAIRS,
            Material.JUNGLE_WOOD_STAIRS
    );
}
