/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.entity;

import org.bukkit.entity.Projectile;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;

public class ProjectileTickEvent extends EntityEvent {

    private static final HandlerList handlers = new HandlerList();
    private final float force;

    public ProjectileTickEvent(final Projectile projectile, final float force) {

        super(projectile);
        this.force = force;
    }

    @Override
    public Projectile getEntity() {

        return (Projectile) super.getEntity();
    }

    public boolean hasLaunchForce() {

        return force != -1;
    }

    public float getLaunchForce() {

        return force;
    }

    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {

        return handlers;
    }
}
