package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Author: Turtle9598
 */
public class PoisonFX extends AbstractEffect {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.POISON, 20 * 600, 2);

    public PoisonFX() {

        super(null, effect);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.POISON;
    }
}
