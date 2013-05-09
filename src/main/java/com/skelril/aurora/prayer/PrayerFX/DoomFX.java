package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Author: Turtle9598
 */
public class DoomFX extends AbstractPrayer {

    private static final AbstractPrayer[] subFX = new AbstractPrayer[] {
            new SlapFX(), new PoisonFX(), new FakeTNTFX()
    };

    public DoomFX() {

        super(subFX);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.DOOM;
    }
}
