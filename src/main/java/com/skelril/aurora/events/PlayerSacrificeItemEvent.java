/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.events;

import com.sk89q.worldedit.blocks.BlockID;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerSacrificeItemEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final Block origin;
    private ItemStack itemStack;


    public PlayerSacrificeItemEvent(final Player player, Block origin, ItemStack itemStack) {

        super(player);
        this.origin = origin;
        this.itemStack = itemStack;
    }

    public Block getBlock() {

        return origin;
    }

    public void setItemStack(ItemStack itemStack) {

        if (itemStack == null) {
            itemStack = new ItemStack(BlockID.AIR);
        }

        this.itemStack = itemStack;
    }

    public ItemStack getItemStack() {

        return itemStack.clone();
    }

    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {

        return handlers;
    }

    public boolean isCancelled() {

        return cancelled;
    }

    public void setCancelled(boolean cancelled) {

        this.cancelled = cancelled;
    }
}
