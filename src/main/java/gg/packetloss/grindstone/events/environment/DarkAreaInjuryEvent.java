/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.environment;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class DarkAreaInjuryEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private int damage;
    private String message;

    public DarkAreaInjuryEvent(Player player, int damage, String message) {

        super(player);
        this.damage = damage;
        this.message = message;
    }

    public int getDamage() {

        return damage;
    }

    public void setDamage(int damage) {

        this.damage = damage;
    }

    public String getMessage() {

        return message;
    }

    public void setMessage(String message) {

        this.message = message;
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