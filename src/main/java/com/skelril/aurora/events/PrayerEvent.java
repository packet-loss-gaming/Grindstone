package com.skelril.aurora.events;

import com.skelril.aurora.prayer.Prayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerEvent;

/**
 * Author: Turtle9598
 */
public abstract class PrayerEvent extends PlayerEvent implements Cancellable {

    private boolean cancelled = false;
    private final Prayer prayer;

    public PrayerEvent(final Player player, Prayer prayer) {

        super(player);
        this.prayer = prayer;
    }

    public Prayer getCause() {

        return prayer;
    }

    public boolean isCancelled() {

        return cancelled;
    }

    public void setCancelled(boolean cancelled) {

        this.cancelled = cancelled;
    }

}
