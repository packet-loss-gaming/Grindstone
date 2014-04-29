/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import com.skelril.aurora.util.DeathUtil;
import org.bukkit.entity.Player;

public class DeadlyPotionFX extends AbstractEffect {
    @Override
    public PrayerType getType() {
        return PrayerType.DEADLYPOTION;
    }

    @Override
    public void add(Player player) {
        DeathUtil.throwSlashPotion(player.getLocation());
    }
}
