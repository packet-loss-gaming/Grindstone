package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class AbsorptionFX extends AbstractPrayer {

    private static final PotionEffect effect = new PotionEffect(PotionEffectType.ABSORPTION, 20 * 600, 2);

    public AbsorptionFX() {

        super(null, effect);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.ABSORPTION;
    }
}