/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class ActionSimulationUtil {
    public static boolean placeBlock(Player player, Block block, BlockState oldState, BlockState newState,
                                     Block placedAgainst, ItemStack itemStack, EquipmentSlot handUsed, boolean canBuild,
                                     Consumer<BlockPlaceEvent> callFunction) {
        newState.update(true, false);

        BlockPlaceEvent event = new BlockPlaceEvent(
            block,
            oldState,
            placedAgainst,
            itemStack.clone(),
            player,
            canBuild,
            handUsed
        );

        callFunction.accept(event);
        if (event.isCancelled()) {
            oldState.update(true, false);
            return false;
        }

        newState.update(true, true);
        return true;
    }
}
