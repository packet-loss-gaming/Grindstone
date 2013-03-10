package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Author: Turtle9598
 */
public class MushroomFX extends AbstractPrayer {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.CONFUSION, 20 * 600, 1);

    public MushroomFX() {

        super(null, effect);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.MUSHROOM;
    }
}
