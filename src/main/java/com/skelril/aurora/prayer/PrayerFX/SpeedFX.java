package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Author: Turtle9598
 */
public class SpeedFX extends AbstractPrayer {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.SPEED, 20 * 600, 2);

    public SpeedFX() {

        super(null, effect);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.SPEED;
    }
}
