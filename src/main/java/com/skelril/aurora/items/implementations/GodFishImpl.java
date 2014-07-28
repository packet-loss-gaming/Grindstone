/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.implementations;

import com.skelril.aurora.items.generic.AbstractItemFeatureImpl;
import com.skelril.aurora.prayer.PrayerFX.HulkFX;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.item.custom.CustomItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class GodFishImpl extends AbstractItemFeatureImpl {
    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {

        Player player = event.getPlayer();
        ItemStack stack = event.getItem();

        if (ItemUtil.isItem(stack, CustomItems.GOD_FISH)) {
            player.chat("The fish flow within me!");
            new HulkFX().add(player);
        }
    }
}
