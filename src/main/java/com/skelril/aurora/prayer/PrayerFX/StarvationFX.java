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
public class StarvationFX extends AbstractEffect {

    @Override
    public PrayerType getType() {

        return PrayerType.STARVATION;
    }

    @Override
    public void add(Player player) {

        if (player.getFoodLevel() > 0) {
            ChatUtil.sendWarning(player, "Tasty...");
            player.setFoodLevel(player.getFoodLevel() - 1);
        }
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
