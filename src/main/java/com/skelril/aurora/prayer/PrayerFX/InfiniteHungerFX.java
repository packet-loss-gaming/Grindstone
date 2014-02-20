/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.entity.Player;

/**
 * Author: Turtle9598
 */
public class InfiniteHungerFX extends AbstractEffect {

    @Override
    public PrayerType getType() {

        return null;
    }

    @Override
    public void add(Player player) {

        player.setFoodLevel(20);
        player.setSaturation(5);
        player.setExhaustion(0);
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
