/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.events.guild;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class NinjaGrappleEvent extends NinjaEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private double maxClimb;

    public NinjaGrappleEvent(Player who, double maxClimb) {
        super(who);
        this.maxClimb = maxClimb;
    }

    public double getMaxClimb() {
        return maxClimb;
    }

    public void setMaxClimb(double maxClimb) {
        this.maxClimb = maxClimb;
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
