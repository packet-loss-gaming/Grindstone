/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerDeathDropRedirectEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    private final Location deathLocation;
    private Location dropLocation;

    public PlayerDeathDropRedirectEvent(Player who, Location dropLocation) {
        super(who);
        this.deathLocation = who.getLocation();
        this.dropLocation = dropLocation;
    }

    public Location getDeathLocation() {
        return deathLocation.clone();
    }

    public Location getDropLocation() {
        return dropLocation.clone();
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
