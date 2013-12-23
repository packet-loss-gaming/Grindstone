package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Author: Turtle9598
 */
public class BlindnessFX extends AbstractEffect {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.BLINDNESS, 20 * 600, 1);

    public BlindnessFX() {

        super(null, effect);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.BLINDNESS;
    }
}
