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
            Material.COBBLESTONE_WALL,
            Material.MOSSY_COBBLESTONE,
            Material.STONE_BRICKS,
            Material.STONE_BRICK_STAIRS,
            Material.BRICK,
            Material.BRICK_STAIRS,
            Material.NETHERRACK,
            Material.NETHER_BRICK,
            Material.NETHER_BRICK_STAIRS,
            Material.NETHER_BRICK_FENCE,
            Material.STONE_SLAB,
            Material.OBSIDIAN,
            Material.GLASS
// FIXME: Add colored glass blocks
//            Material.STAINED_GLASS,
//            Material.THIN_GLASS,
//            Material.STAINED_GLASS_PANE
    );
}
