package com.skelril.aurora.prayer.PrayerFX;

import com.skelril.aurora.prayer.PrayerType;

/**
 * Author: Turtle9598
 */
public class DoomFX extends AbstractEffect {

    private static final AbstractEffect[] subFX = new AbstractEffect[]{
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
