/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.events.guild;

import com.sk89q.worldedit.event.Cancellable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.List;

public class NinjaTormentArrowEvent extends NinjaEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private List<Entity> entities;

    public NinjaTormentArrowEvent(Player who, List<Entity> entities) {
        super(who);
        this.entities = entities;
    }

    public List<Entity> getEntities() {
        return entities;
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
