/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HealthBoostFX extends AbstractEffect {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.HEALTH_BOOST, 20 * 600, 4);

    public HealthBoostFX() {

        super(null, effect);
    }

    @Override
    public void clean(Player player) {

        // Don't clean this effect, it messes it up
    }

    @Override
    public void kill(Player player) {

        player.removePotionEffect(PotionEffectType.HEALTH_BOOST);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.HEALTHBOOST;
    }
}