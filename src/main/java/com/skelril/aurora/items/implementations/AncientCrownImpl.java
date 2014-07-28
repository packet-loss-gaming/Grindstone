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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class AncientCrownImpl extends AbstractCondenserImpl {

    public AncientCrownImpl(ItemCondenser condenser) {
        super(condenser);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {

        Entity healed = event.getEntity();

        if (healed instanceof Player) {

            Player player = (Player) healed;

            if (ItemUtil.isItem(player.getInventory().getHelmet(), CustomItems.ANCIENT_CROWN)) {
                event.setAmount(event.getAmount() * 2.5);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onXPPickUp(PlayerExpChangeEvent event) {

        Player player = event.getPlayer();

        if (ItemUtil.isItem(player.getInventory().getHelmet(), CustomItems.ANCIENT_CROWN)) {
            event.setAmount(event.getAmount() * 2);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemPickup(PlayerPickupItemEvent event) {

        final Player player = event.getPlayer();
        ItemStack itemStack = event.getItem().getItemStack();

        if (condenser.supports(itemStack)) {

            if (!ItemUtil.isItem(player.getInventory().getHelmet(), CustomItems.ANCIENT_CROWN)) {
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
