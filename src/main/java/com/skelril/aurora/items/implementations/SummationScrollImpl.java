/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.implementations;

import com.skelril.aurora.items.generic.AbstractCondenserImpl;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.ItemCondenser;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.item.custom.CustomItemCenter;
import com.skelril.aurora.util.item.custom.CustomItems;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SummationScrollImpl extends AbstractCondenserImpl {

    public SummationScrollImpl(ItemCondenser condenser) {
        super(condenser);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            // Scrolls
            boolean isScrollOfSummation = ItemUtil.isItem(itemStack, CustomItems.SCROLL_OF_SUMMATION);
            if (isScrollOfSummation) {
                ItemStack[] result = condenser.operate(player.getInventory().getContents());
                if (result != null) {
                    player.getInventory().setContents(result);
                    ItemUtil.removeItemOfName(player, CustomItemCenter.build(CustomItems.SCROLL_OF_SUMMATION), 1, false);
                    ChatUtil.sendNotice(player, ChatColor.GOLD, "The scroll glows brightly before turning to dust...");
                }
            }
        }
    }
}
