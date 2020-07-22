/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations.combotools;

import gg.packetloss.hackbook.MaterialInfoUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;

class ComboUtil {
    public static boolean isBreakableWithAxe(Block block) {
        return MaterialInfoUtil.getBreakSpeed(Material.NETHERITE_AXE, block) > 1.0F;
    }

    public static boolean isBreakableWithPickaxe(Block block) {
        return MaterialInfoUtil.getBreakSpeed(Material.NETHERITE_PICKAXE, block) > 1.0F;
    }

    public static boolean isBreakableWithShovel(Block block) {
        return MaterialInfoUtil.getBreakSpeed(Material.NETHERITE_SHOVEL, block) > 1.0F;
    }
}
