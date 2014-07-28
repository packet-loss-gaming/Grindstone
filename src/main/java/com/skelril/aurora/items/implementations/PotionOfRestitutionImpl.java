/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.implementations;

import com.skelril.aurora.items.generic.AbstractItemFeatureImpl;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.item.custom.CustomItems;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

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
            Location lastLoc = getSession(player).getRecentDeathPoint();
            if (lastLoc != null) {
                if (!player.teleport(lastLoc)) {
                    ChatUtil.sendError(player, "Location Information: X: "
                                    + lastLoc.getBlockX() + ", Y: "
                                    + lastLoc.getBlockY() + ", Z: "
                                    + lastLoc.getBlockZ() + " in "
                                    + lastLoc.getWorld().getName() + '.'
                    );
                }
            } else {
                ChatUtil.sendError(player, "No previous death points are known the the potion.");
            }
        }
    }
}
