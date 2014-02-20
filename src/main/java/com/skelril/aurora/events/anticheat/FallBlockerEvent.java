/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.events.anticheat;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Author: Turtle9598
 */
public class FallBlockerEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();
    private boolean displayMessage = true;

    public FallBlockerEvent(Player player) {

        super(player);
    }

    public boolean isDisplayingMessage() {

        return displayMessage;
    }

    public void setDisplayMessage(boolean displayMessage) {

        this.displayMessage = displayMessage;
    }

    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {

        return handlers;
    }
}
