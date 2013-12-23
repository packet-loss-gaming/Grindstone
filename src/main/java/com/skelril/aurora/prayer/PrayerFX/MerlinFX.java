package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;

/**
 * Author: Turtle9598
 */
public class MerlinFX extends AbstractEffect {

    private static final AbstractEffect[] subFX = new AbstractEffect[]{
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
