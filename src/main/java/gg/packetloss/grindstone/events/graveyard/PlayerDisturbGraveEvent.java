/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.graveyard;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import java.util.Collections;
import java.util.List;

public class PlayerDisturbGraveEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final World world;
    private final List<BlockVector3> consideredGraves;
    private final List<BlockVector3> disturbedGraves;
    private boolean cancelled = false;

    public PlayerDisturbGraveEvent(Player who, List<BlockVector3> consideredGraves, List<BlockVector3> disturbedGraves) {
        super(who);
        this.world = who.getWorld();
        this.consideredGraves = consideredGraves;
        this.disturbedGraves = disturbedGraves;
    }

    public World getWorld() {
        return world;
    }

    public List<BlockVector3> getConsideredGraves() {
        return Collections.unmodifiableList(consideredGraves);
    }

    public List<BlockVector3> getDisturbedGraves() {
        return disturbedGraves;
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

