/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Author: Turtle9598
 */
public class ServerShutdownEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final int secondsLeft;

    public ServerShutdownEvent(int secondsLeft) {

        this.secondsLeft = secondsLeft;
    }

    public int getSecondsLeft() {

        return secondsLeft;
    }

    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {

        return handlers;
    }
}
