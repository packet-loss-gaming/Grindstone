/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.apocalypse;

import com.sk89q.worldedit.math.BlockVector2;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ApocalypseStartEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private World world;
    private BlockVector2 centralPoint;
    private BlockVector2 boundingBox;
    private boolean cancelled = false;

    public ApocalypseStartEvent(World world, BlockVector2 centralPoint, BlockVector2 boundingBox) {
        this.world = world;
        this.centralPoint = centralPoint;
        this.boundingBox = boundingBox;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    /**
     * @return the focal point of the impending apocalypse
     */
    public BlockVector2 getCentralPoint() {
        return centralPoint;
    }

    public void setCentralPoint(BlockVector2 centralPoint) {
        this.centralPoint = centralPoint;
    }

    public BlockVector2 getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(BlockVector2 boundingBox) {
        this.boundingBox = boundingBox;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

