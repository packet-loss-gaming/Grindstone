/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import com.skelril.aurora.util.EnvironmentUtil;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Author: Turtle9598
 */
public class SmokeFX extends AbstractEffect {

    @Override
    public PrayerType getType() {

        return PrayerType.SMOKE;
    }

    @Override
    public void add(Player player) {

        Location[] smoke = new Location[2];
        smoke[0] = player.getLocation();
        smoke[1] = player.getEyeLocation();
        EnvironmentUtil.generateRadialEffect(smoke, Effect.SMOKE);
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
