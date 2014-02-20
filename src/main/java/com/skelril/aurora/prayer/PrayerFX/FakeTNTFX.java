/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class FakeTNTFX extends AbstractEffect {

    @Override
    public PrayerType getType() {

        return null;
    }

    @Override
    public void add(Player player) {

        Location playerLoc = player.getLocation();
        player.getWorld().createExplosion(playerLoc.getX(), playerLoc.getY(), playerLoc.getZ(), 0, false, false);
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}