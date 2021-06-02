/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations.combotools;

import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.items.implementations.support.LinearDestructionExecutor;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;

public class LinearAxe extends AbstractItemFeatureImpl {
    private static LinearDestructionExecutor executor = new LinearDestructionExecutor(CustomItems.LINEAR_AXE) {
        @Override
        public boolean accepts(Block block) {
            return ComboUtil.isBreakableWithAxe(block);
        }
    };

    @EventHandler(ignoreCancelled = true)
    public void onBlockInteract(PlayerInteractEvent event) {
        executor.process(event);
    }

    @Override
    public boolean onItemRightClick(PlayerInteractEvent event) {
        return executor.impedeRightClick(event.getPlayer());
    }
}
