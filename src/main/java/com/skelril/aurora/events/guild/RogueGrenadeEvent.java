/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.events.guild;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class RogueGrenadeEvent extends RogueEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private int grenadeCount;

    public RogueGrenadeEvent(Player who, int grenadeCount) {
        super(who);
        this.grenadeCount = grenadeCount;
    }

    public int getGrenadeCount() {
        return grenadeCount;
    }

    public void setGrenadeCount(int grenadeCount) {
        this.grenadeCount = grenadeCount;
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
