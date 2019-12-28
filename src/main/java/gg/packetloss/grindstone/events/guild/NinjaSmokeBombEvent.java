/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.guild;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

public class NinjaSmokeBombEvent extends NinjaPowerUseEvent {
    private int explosionPower;
    private int delay;
    private List<Entity> entities;
    private Location originTeleportLoc;
    private Location teleportLoc;
    private Location targetLoc;

    public NinjaSmokeBombEvent(Player who, int explosionPower, int delay,
                               List<Entity> entities, Location teleportLoc, Location targetLoc) {
        super(who);
        this.explosionPower = explosionPower;
        this.delay = delay;
        this.entities = entities;
        this.originTeleportLoc = this.teleportLoc = teleportLoc;
        this.targetLoc = targetLoc;
    }

    public int getExplosionPower() {
        return explosionPower;
    }

    public void setExplosionPower(int explosionPower) {
        this.explosionPower = explosionPower;
    }

    public int getDelay() {
        return delay;
    }

    public long getDelayInMills() {
        return (delay * 1000) / 20;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public Location getOriginalTeleportLoc() {
        return originTeleportLoc.clone();
    }

    public Location getTeleportLoc() {
        return teleportLoc.clone();
    }

    public void setTeleportLoc(Location teleportLoc) {
        this.teleportLoc = teleportLoc.clone();
    }

    public Location getTargetLoc() {
        return targetLoc.clone();
    }

    public void setTargetLoc(Location targetLoc) {
        this.targetLoc = targetLoc.clone();
    }
}
