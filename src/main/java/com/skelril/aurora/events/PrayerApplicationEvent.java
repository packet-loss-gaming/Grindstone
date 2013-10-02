package com.skelril.aurora.events;

import com.skelril.aurora.prayer.Prayer;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Author: Turtle9598
 */
public class PrayerApplicationEvent extends PrayerEvent {

    private static final HandlerList handlers = new HandlerList();


    public PrayerApplicationEvent(final Player player, Prayer prayer) {

        super(player, prayer);
    }

    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {

        return handlers;
    }
}