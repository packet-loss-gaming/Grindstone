package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Author: Turtle9598
 */
public class HealthFX extends AbstractEffect {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.REGENERATION, 20 * 600, 1);

    public HealthFX() {

        super(null, effect);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.HEALTH;
    }
}
