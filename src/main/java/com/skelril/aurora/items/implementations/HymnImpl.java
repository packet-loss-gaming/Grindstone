/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.implementations;

import com.skelril.aurora.events.custom.item.HymnSingEvent;
import com.skelril.aurora.items.generic.AbstractItemFeatureImpl;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.item.custom.CustomItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class HymnImpl extends AbstractItemFeatureImpl {

    private Map<CustomItems, HymnSingEvent.Hymn> hymns = new HashMap<>();

    public void addHymn(CustomItems item, HymnSingEvent.Hymn hymn) {
        hymns.put(item, hymn);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            for (Map.Entry<CustomItems, HymnSingEvent.Hymn> entry : hymns.entrySet()) {
                if (ItemUtil.isItem(itemStack, entry.getKey())) {
                    //noinspection AccessStaticViaInstance
                    inst.callEvent(new HymnSingEvent(player, entry.getValue()));
                    break;
                }
            }
        }
    }
}
