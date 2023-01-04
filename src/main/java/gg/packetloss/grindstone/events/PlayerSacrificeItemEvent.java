/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events;

import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerSacrificeItemEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final Location origin;
    private ItemStack itemStack;


    public PlayerSacrificeItemEvent(final Player player, Location origin, ItemStack itemStack) {

        super(player);
        this.origin = origin;
        this.itemStack = itemStack;
    }

    public Location getPointOfSacrifice() {
        return origin.clone();
    }

    public void setItemStack(ItemStack itemStack) {
        if (itemStack == null) {
            itemStack = ItemUtil.newNullItemstack();
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
