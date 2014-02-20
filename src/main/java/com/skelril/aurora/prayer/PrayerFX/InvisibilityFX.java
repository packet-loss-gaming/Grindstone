/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Author: Turtle9598
 */
public class InvisibilityFX extends AbstractEffect {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 600, 1);

    public InvisibilityFX() {

        super(null, effect);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.INVISIBILITY;
    }
}
