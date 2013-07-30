package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;

/**
 * Author: Turtle9598
 */
public class MerlinFX extends AbstractPrayer {

    private static final AbstractPrayer[] subFX = new AbstractPrayer[]{
            new FireFX(), new BlindnessFX(), new SmokeFX(), new MushroomFX(), new ButterFingersFX()
    };

    public MerlinFX() {

        super(subFX);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.MERLIN;
    }
}
