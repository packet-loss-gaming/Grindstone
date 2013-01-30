package us.arrowcraft.aurora.prayer;
import us.arrowcraft.aurora.prayer.PrayerFX.AlonzoFX;
import us.arrowcraft.aurora.prayer.PrayerFX.AntifireFX;
import us.arrowcraft.aurora.prayer.PrayerFX.ArrowFX;
import us.arrowcraft.aurora.prayer.PrayerFX.BlindnessFX;
import us.arrowcraft.aurora.prayer.PrayerFX.ButterFingersFX;
import us.arrowcraft.aurora.prayer.PrayerFX.CannonFX;
import us.arrowcraft.aurora.prayer.PrayerFX.DeadlyDefenseFX;
import us.arrowcraft.aurora.prayer.PrayerFX.DoomFX;
import us.arrowcraft.aurora.prayer.PrayerFX.FireFX;
import us.arrowcraft.aurora.prayer.PrayerFX.FlashFX;
import us.arrowcraft.aurora.prayer.PrayerFX.GodFX;
import us.arrowcraft.aurora.prayer.PrayerFX.HealthFX;
import us.arrowcraft.aurora.prayer.PrayerFX.InventoryFX;
import us.arrowcraft.aurora.prayer.PrayerFX.InvisibilityFX;
import us.arrowcraft.aurora.prayer.PrayerFX.MushroomFX;
import us.arrowcraft.aurora.prayer.PrayerFX.NightVisionFX;
import us.arrowcraft.aurora.prayer.PrayerFX.PoisonFX;
import us.arrowcraft.aurora.prayer.PrayerFX.PowerFX;
import us.arrowcraft.aurora.prayer.PrayerFX.RacketFX;
import us.arrowcraft.aurora.prayer.PrayerFX.RocketFX;
import us.arrowcraft.aurora.prayer.PrayerFX.SlapFX;
import us.arrowcraft.aurora.prayer.PrayerFX.SpeedFX;
import us.arrowcraft.aurora.prayer.PrayerFX.StarvationFX;
import us.arrowcraft.aurora.prayer.PrayerFX.TNTFX;
import us.arrowcraft.aurora.prayer.PrayerFX.ThrownFireballFX;
import us.arrowcraft.aurora.prayer.PrayerFX.WalkFX;
import us.arrowcraft.aurora.prayer.PrayerFX.ZombieFX;

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
    ALONZO(1016, AlonzoFX.class),
    ROCKET(1017, RocketFX.class),

    FIREBALL(2000, ThrownFireballFX.class),
    HEALTH(2001, HealthFX.class),
    SPEED(2002, SpeedFX.class),
    ANTIFIRE(2003, AntifireFX.class),
    POWER(2004, PowerFX.class),
    GOD(2005, GodFX.class),
    NIGHTVISION(2006, NightVisionFX.class),
    FLASH(2007, FlashFX.class),
    INVISIBILITY(2008, InvisibilityFX.class),
    DEADLYDEFENSE(2009, DeadlyDefenseFX.class);

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
