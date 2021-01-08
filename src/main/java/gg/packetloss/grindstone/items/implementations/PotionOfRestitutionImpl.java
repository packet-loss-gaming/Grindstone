/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class PotionOfRestitutionImpl extends AbstractItemFeatureImpl {
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        getSession(player).addDeathPoint(player.getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack stack = event.getItem();

        if (ItemUtil.isItem(stack, CustomItems.POTION_OF_RESTITUTION)) {
            Optional<Location> optLastLoc = getSession(player).getRecentDeathPoint();
            if (optLastLoc.isEmpty()) {
                ChatUtil.sendError(player, "No previous death points are known the the potion.");
                event.setCancelled(true);
                return;
            }

            Location lastLoc = optLastLoc.get();
            if (!player.teleport(lastLoc)) {
                ChatUtil.sendError(player, "Location Information: X: "
                                + lastLoc.getBlockX() + ", Y: "
                                + lastLoc.getBlockY() + ", Z: "
                                + lastLoc.getBlockZ() + " in "
                                + lastLoc.getWorld().getName() + '.'
                );
            }
        }
    }
}
