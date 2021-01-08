/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.graveyard;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerCreateGraveEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    private final Location deathLocation;
    private final Location graveLocation;

    public PlayerCreateGraveEvent(Player who, Location graveLocation) {
        super(who);
        this.deathLocation = who.getLocation();
        this.graveLocation = graveLocation;
    }

    public Location getDeathLocation() {
        return deathLocation.clone();
    }

    public Location getGraveLocation() {
        return graveLocation.clone();
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
