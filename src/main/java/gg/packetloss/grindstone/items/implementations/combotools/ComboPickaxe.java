/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations.combotools;

import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import org.bukkit.Material;

import java.util.Set;

public class ComboPickaxe extends AbstractItemFeatureImpl {
    protected static Set<Material> acceptedMaterials = Set.of(
            Material.STONE,
            Material.SANDSTONE,
            Material.SANDSTONE_STAIRS,
            Material.GLOWSTONE,
            Material.COBBLESTONE,
            Material.COBBLESTONE_STAIRS,
            Material.COBBLE_WALL,
            Material.MOSSY_COBBLESTONE,
            Material.SMOOTH_BRICK,
            Material.SMOOTH_STAIRS,
            Material.BRICK,
            Material.BRICK_STAIRS,
            Material.NETHERRACK,
            Material.NETHER_BRICK,
            Material.NETHER_BRICK_STAIRS,
            Material.NETHER_FENCE,
            Material.STEP,
            Material.DOUBLE_STEP,
            Material.OBSIDIAN,
            Material.GLASS,
            Material.STAINED_GLASS,
            Material.THIN_GLASS,
            Material.STAINED_GLASS_PANE
    );
}
