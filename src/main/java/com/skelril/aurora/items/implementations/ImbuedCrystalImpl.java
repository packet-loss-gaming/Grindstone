/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.implementations;

import com.skelril.aurora.items.generic.AbstractCondenserImpl;
import com.skelril.aurora.util.ItemCondenser;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.item.custom.CustomItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class ImbuedCrystalImpl extends AbstractCondenserImpl {

    public ImbuedCrystalImpl(ItemCondenser condenser) {
        super(condenser);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemPickup(PlayerPickupItemEvent event) {

        final Player player = event.getPlayer();
        ItemStack itemStack = event.getItem().getItemStack();

        if (condenser.supports(itemStack)) {

            if (!ItemUtil.hasItem(player, CustomItems.IMBUED_CRYSTAL)) {
                return;
            }

            server.getScheduler().runTaskLater(inst, () -> {
                ItemStack[] result = condenser.operate(player.getInventory().getContents());
                if (result != null) {
                    player.getInventory().setContents(result);
                    //noinspection deprecation
                    player.updateInventory();
                }
            }, 1);
        }
    }
}
