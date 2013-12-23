package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Author: Turtle9598
 */
public class NightVisionFX extends AbstractEffect {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 600, 1);

    public NightVisionFX() {

        super(null, effect);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.NIGHTVISION;
    }
}
