/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class MadMilkImpl extends AbstractItemFeatureImpl {
    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {

        Player player = event.getPlayer();
        ItemStack stack = event.getItem();

        if (ItemUtil.isItem(stack, CustomItems.MAD_MILK)) {
            server.getScheduler().runTaskLater(inst, () -> player.getInventory().setItemInHand(CustomItemCenter.build(CustomItems.MAGIC_BUCKET)), 1);
            event.setCancelled(true);
        }
    }
}
