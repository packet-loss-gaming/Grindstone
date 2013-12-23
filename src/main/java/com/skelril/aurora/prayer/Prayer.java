package com.skelril.aurora.prayer;

import com.skelril.aurora.prayer.PrayerFX.AbstractEffect;
import com.skelril.aurora.prayer.PrayerFX.AbstractTriggeredEffect;
import org.bukkit.entity.Player;

/**
 * Author: Turtle9598
 */
public class Prayer implements Comparable<Prayer> {

    private final Player player;
    private final AbstractEffect abstractEffect;
    private final long startTime;
    private long maxDuration;


    protected Prayer(Player player, AbstractEffect abstractEffect, long maxDuration) {

        this.player = player;
        this.abstractEffect = abstractEffect;
        this.startTime = System.currentTimeMillis();
        this.maxDuration = maxDuration;
    }

    public Player getPlayer() {

        return player;
    }

    public AbstractEffect getEffect() {

        return abstractEffect;
    }

    public long getStartTime() {

        return startTime;
    }

    public long getMaxDuration() {

        return maxDuration;
    }

    public void setMaxDuration(long maxDuration) {

        this.maxDuration = maxDuration;
    }

    public boolean hasTrigger() {

        return abstractEffect instanceof AbstractTriggeredEffect;
    }

    public Class getTriggerClass() {

        return hasTrigger() ? ((AbstractTriggeredEffect) abstractEffect).getTriggerClass() : null;
    }

    @Override
    public int compareTo(Prayer prayer) {

        if (prayer == null) return 0;

        if (this.getEffect().getType().getValue() == prayer.getEffect().getType().getValue()) return 0;
        if (this.getEffect().getType().getValue() > prayer.getEffect().getType().getValue()) return 1;
        return -1;
    }
}
