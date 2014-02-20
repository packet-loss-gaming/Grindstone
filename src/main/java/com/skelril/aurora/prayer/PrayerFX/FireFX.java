/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import com.skelril.aurora.util.ChatUtil;
import org.bukkit.entity.Player;

/**
 * Author: Turtle9598
 */
public class FireFX extends AbstractEffect {

    @Override
    public PrayerType getType() {

        return PrayerType.FIRE;
    }

    @Override
    public void add(Player player) {

        if (player.getFireTicks() < 20) {
            ChatUtil.sendWarning(player, "BURN!!!");
            player.setFireTicks((20 * 60));
        }
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
