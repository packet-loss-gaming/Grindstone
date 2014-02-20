/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.events.entity;

import org.bukkit.entity.Projectile;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;

/**
 * Author: Turtle9598
 */
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
