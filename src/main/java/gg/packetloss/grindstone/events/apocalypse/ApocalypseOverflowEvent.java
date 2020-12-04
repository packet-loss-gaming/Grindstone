/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.apocalypse;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class ApocalypseOverflowEvent extends PlayerEvent implements ApocalypseEvent {
    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled = false;
    private Location spawnLocation;
    private int killChance = 1;
    private boolean spawnMerciless = true;

    public ApocalypseOverflowEvent(final Player player, Location spawnLocation) {
        super(player);
        this.spawnLocation = spawnLocation;
    }

    /**
     * @return the location of the merciless zombie spawn - if not prevented
     */
    @Override
    public Location getLocation() {
        return spawnLocation;
    }

    /**
     * @param spawnLocation
     */
    public void setLocation(Location spawnLocation) {
        Validate.notNull(spawnLocation);
        this.spawnLocation = spawnLocation;
    }

    /**
     * @return the chance of a zombie being killed (i.e. 1 / kill chance)
     */
    public int getKillChance() {
        return killChance;
    }

    public void setKillChance(int killChance) {
        this.killChance = killChance;
    }

    public boolean shouldSpawnMerciless() {
        return spawnMerciless;
    }

    public void setSpawnMerciless(boolean spawnMerciless) {
        this.spawnMerciless = spawnMerciless;
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
