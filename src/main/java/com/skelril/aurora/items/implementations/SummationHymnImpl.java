/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.implementations;

import com.skelril.aurora.events.custom.item.HymnSingEvent;
import com.skelril.aurora.items.generic.AbstractCondenserImpl;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.ItemCondenser;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;

public class SummationHymnImpl extends AbstractCondenserImpl {

    public SummationHymnImpl(ItemCondenser condenser) {
        super(condenser);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHymnSing(HymnSingEvent event) {
        Player player = event.getPlayer();
        HymnSingEvent.Hymn hymn = event.getHymn();

        switch (hymn) {
            case SUMMATION:
                ItemStack[] result = condenser.operate(player.getInventory().getContents());
                if (result != null) {
                    player.getInventory().setContents(result);
                    //noinspection deprecation
                    player.updateInventory();
                    ChatUtil.sendNotice(player, ChatColor.GOLD, "The hymn glows brightly...");
                }
                break;
        }
    }
}
