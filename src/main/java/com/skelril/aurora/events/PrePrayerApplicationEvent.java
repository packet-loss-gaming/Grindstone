/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.events;

import com.skelril.aurora.prayer.Prayer;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Author: Turtle9598
 */
public class PrePrayerApplicationEvent extends PrayerEvent {

    private static final HandlerList handlers = new HandlerList();


    public PrePrayerApplicationEvent(final Player player, Prayer prayer) {

        super(player, prayer);
    }

    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {

        return handlers;
    }
}
