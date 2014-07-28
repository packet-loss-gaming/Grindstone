/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.generic;

import com.skelril.aurora.util.CollectionUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;

public abstract class AbstractXPArmor extends AbstractItemFeatureImpl {

    public abstract boolean hasArmor(Player player);

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onXPPickUp(PlayerExpChangeEvent event) {

        Player player = event.getPlayer();

        int exp = event.getAmount();

        if (hasArmor(player)) {
            ItemStack[] armour = player.getInventory().getArmorContents();
            ItemStack is = CollectionUtil.getElement(armour);
            if (exp > is.getDurability()) {
                exp -= is.getDurability();
                is.setDurability((short) 0);
            } else {
                is.setDurability((short) (is.getDurability() - exp));
                exp = 0;
            }
            player.getInventory().setArmorContents(armour);
            event.setAmount(exp);
        }
    }
}
