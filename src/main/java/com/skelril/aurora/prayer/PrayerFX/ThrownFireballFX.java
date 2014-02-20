/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.Location;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Author: Turtle9598
 */
public class ThrownFireballFX extends AbstractTriggeredEffect {

    private long nextTime = -1;

    public ThrownFireballFX() {

        super(PlayerInteractEvent.class);
    }

    @Override
    public void trigger(Player player) {

        if (nextTime != -1 && System.currentTimeMillis() < nextTime) return;

        Location loc = player.getEyeLocation().toVector().add(player.getLocation().getDirection().multiply(2))
                .toLocation(player.getWorld(), player.getLocation().getYaw(), player.getLocation().getPitch());
        player.getWorld().spawn(loc, Fireball.class);

        nextTime = System.currentTimeMillis() + 750;
    }

    @Override
    public PrayerType getType() {

        return PrayerType.FIREBALL;
    }
}
