package com.skelril.aurora.prayer;

import com.skelril.aurora.prayer.PrayerFX.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: Turtle9598
 */
public enum PrayerType {

    UNASSIGNED(0, null),

    FIRE(1000, FireFX.class),
    STARVATION(1001, StarvationFX.class),
    INVENTORY(1002, InventoryFX.class),
    RACKET(1003, RacketFX.class),
    SMOKE(1004, StarvationFX.class),
    TNT(1005, TNTFX.class),
    WALK(1006, WalkFX.class),
    SLAP(1007, SlapFX.class),
    BUTTERFINGERS(1008, ButterFingersFX.class),
    ARROW(1009, ArrowFX.class),
    ZOMBIE(1010, ZombieFX.class),
    DOOM(1011, DoomFX.class),
    POISON(1012, PoisonFX.class),
    BLINDNESS(1013, BlindnessFX.class),
    MUSHROOM(1014, MushroomFX.class),
    CANNON(1015, CannonFX.class),
    MERLIN(1016, MerlinFX.class),
    ROCKET(1017, RocketFX.class),
    GLASSBOX(1018, GlassBoxFX.class),

    FIREBALL(2000, ThrownFireballFX.class),
    HEALTH(2001, HealthFX.class),
    SPEED(2002, SpeedFX.class),
    ANTIFIRE(2003, AntifireFX.class),
    POWER(2004, PowerFX.class),
    GOD(2005, GodFX.class),
    NIGHTVISION(2006, NightVisionFX.class),
    FLASH(2007, FlashFX.class),
    INVISIBILITY(2008, InvisibilityFX.class),
    DEADLYDEFENSE(2009, DeadlyDefenseFX.class),
    DIGGYDIGGY(2010, DiggyDiggyFX.class),
    HULK(2011, HulkFX.class),
    HEALTHBOOST(2012, HealthBoostFX.class),
    ABSORPTION(2013, AbsorptionFX.class);

    private final int id;
    private final Class FXClass;
    private final static Map<Integer, PrayerType> BY_ID = new HashMap<>();

    private PrayerType(int id, Class FXClass) {

        this.id = id;
        this.FXClass = FXClass;
    }

    public int getValue() {

        return id;
    }

    public Class getFXClass() {

        return FXClass;
    }

    public boolean isHoly() {

        return getValue() >= 2000;
    }

    public boolean isUnholy() {

        return getValue() < 2000;
    }

    public static PrayerType getId(final int id) {

        return BY_ID.get(id);
    }

    static {
        for (PrayerType prayerType : values()) {
            BY_ID.put(prayerType.getValue(), prayerType);
        }
    }
}
