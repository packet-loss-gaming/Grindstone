/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations.combotools;

import org.bukkit.Tag;
import org.bukkit.block.Block;

class ComboUtil {
    public static boolean isBreakableWithAxe(Block block) {
        return Tag.MINEABLE_AXE.isTagged(block.getType());
    }

    public static boolean isBreakableWithPickaxe(Block block) {
        return Tag.MINEABLE_PICKAXE.isTagged(block.getType());
    }

    public static boolean isBreakableWithShovel(Block block) {
        return Tag.MINEABLE_SHOVEL.isTagged(block.getType());
    }
}
