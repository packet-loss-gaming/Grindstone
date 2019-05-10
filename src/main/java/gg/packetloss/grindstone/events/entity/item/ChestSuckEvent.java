/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.entity.item;

import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class ChestSuckEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final Block container;
    private final Item item;
    private ItemStack itemStack;

    public ChestSuckEvent(Block container, Item item, ItemStack itemStack) {

        this.container = container;
        this.item = item;
        this.itemStack = itemStack;
    }

    public Block getContainer() {

        return container;
    }

    public Item getItem() {

        return item;
    }

    public ItemStack getItemStack() {

        return itemStack;
    }

    public void setItemStack(ItemStack itemStack) {

        this.itemStack = itemStack;
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
