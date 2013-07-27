package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HealthBoostFX extends AbstractPrayer {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.HEALTH_BOOST, 20 * 600, 2);

    public HealthBoostFX() {

        super(null, effect);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.HEALTHBOOST;
    }
}