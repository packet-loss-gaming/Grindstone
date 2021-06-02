/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.apocalypse;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ApocalypseLightningStrikeSpawnEvent extends Event implements ApocalypseEvent {
    private static final HandlerList handlers = new HandlerList();

    private final Location triggeringLocation;
    private final Location location;
    private int numZombies;

    private boolean cancelled = false;

    public ApocalypseLightningStrikeSpawnEvent(Location triggeringLocation, Location location, int numZombies) {
        this.triggeringLocation = triggeringLocation;
        this.location = location;
        this.numZombies = numZombies;
    }

    public Location getTriggeringLocation() {
        return triggeringLocation.clone();
    }

    @Override
    public Location getLocation() {
        return location.clone();
    }

    public int getNumberOfZombies() {
        return numZombies;
    }

    public void setNumberOfZombies(int numZombies) {
        this.numZombies = numZombies;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
