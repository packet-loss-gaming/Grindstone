package com.skelril.aurora.prayer;

import com.skelril.aurora.prayer.PrayerFX.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Author: Turtle9598
 */
public enum PrayerType {

    UNASSIGNED(0, 0, null),

    FIRE(1000, 35, FireFX.class),
    STARVATION(1001, 35, StarvationFX.class),
    INVENTORY(1002, 35, InventoryFX.class),
    RACKET(1003, 35, RacketFX.class),
    SMOKE(1004, 35, StarvationFX.class),
    TNT(1005, 35, TNTFX.class),
    WALK(1006, 35, WalkFX.class),
    SLAP(1007, 35, SlapFX.class),
    BUTTERFINGERS(1008, 35, ButterFingersFX.class),
    ARROW(1009, 35, ArrowFX.class),
    ZOMBIE(1010, 35, ZombieFX.class),
    DOOM(1011, 35, DoomFX.class),
    POISON(1012, 35, PoisonFX.class),
    BLINDNESS(1013, 35, BlindnessFX.class),
    MUSHROOM(1014, 35, MushroomFX.class),
    CANNON(1015, 35, CannonFX.class),
    MERLIN(1016, 35, MerlinFX.class),
    ROCKET(1017, 35, 1, RocketFX.class),
    GLASSBOX(1018, 35, GlassBoxFX.class),

    FIREBALL(2000, 75, ThrownFireballFX.class),
    HEALTH(2001, 15, HealthFX.class),
    SPEED(2002, 20, SpeedFX.class),
    ANTIFIRE(2003, 15, AntifireFX.class),
    POWER(2004, 50, PowerFX.class),
    GOD(2005, 150, GodFX.class),
    NIGHTVISION(2006, 30, NightVisionFX.class),
    FLASH(2007, 40, FlashFX.class),
    INVISIBILITY(2008, 30, InvisibilityFX.class),
    DEADLYDEFENSE(2009, 75, DeadlyDefenseFX.class),
    DIGGYDIGGY(2010, 30, DiggyDiggyFX.class),
    HULK(2011, 50, HulkFX.class),
    HEALTHBOOST(2012, 15, HealthBoostFX.class),
    ABSORPTION(2013, 15, AbsorptionFX.class);

    private final int id;
    private final int cost;
    private final long defaultTime;
    private final Class FXClass;
    private final static Map<Integer, PrayerType> BY_ID = new HashMap<>();

    private PrayerType(int id, int cost, Class FXClass) {

        this.id = id;
        this.cost = cost;
        this.defaultTime = TimeUnit.MINUTES.toMillis(15);
        this.FXClass = FXClass;
    }

    private PrayerType(int id, int cost, int defaultTime, Class FXClass) {

        this.id = id;
        this.cost = cost;
        this.defaultTime = TimeUnit.MINUTES.toMillis(defaultTime);
        this.FXClass = FXClass;
    }

    public int getValue() {

        return id;
    }

    public int getLevelCost() {

        return cost;
    }

    public long getDefaultTime() {

        return defaultTime;
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
