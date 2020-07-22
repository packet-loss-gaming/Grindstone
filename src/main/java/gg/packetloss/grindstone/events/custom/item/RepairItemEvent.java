/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.custom.item;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

public class RepairItemEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    private final ItemStack itemStack;
    private float repairPercentage;

    public RepairItemEvent(Player who, ItemStack itemStack, float repairPercentage) {
        super(who);
        this.itemStack = itemStack;
        setRepairPercentage(repairPercentage);
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public float getRepairPercentage() {
        return repairPercentage;
    }

    public void setRepairPercentage(float repairPercentage) {
        Validate.isTrue(0 <= repairPercentage && repairPercentage <= 1);
        this.repairPercentage = repairPercentage;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

