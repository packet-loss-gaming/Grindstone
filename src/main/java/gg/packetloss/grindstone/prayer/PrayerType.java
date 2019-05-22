/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer;

import gg.packetloss.grindstone.prayer.impl.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public enum PrayerType {

  UNASSIGNED(0, 0, null),

  FIRE(1000, 35, FirePrayer.class),
  STARVATION(1001, 35, StarvationPrayer.class),
  INVENTORY(1002, 35, InventoryPrayer.class),
  RACKET(1003, 35, RacketPrayer.class),
  SMOKE(1004, 35, SmokeEffectPrayer.class),
  TNT(1005, 35, TNTPrayer.class),
  WALK(1006, 35, WalkPrayer.class),
  SLAP(1007, 35, SlapPrayer.class),
  BUTTERFINGERS(1008, 35, BufferFingersPrayer.class),
  ARROW(1009, 35, ArrowPrayer.class),
  ZOMBIE(1010, 35, ZombiePrayer.class),
  DOOM(1011, 35, DoomPrayer.class),
  POISON(1012, 35, PoisonPrayer.class),
  BLINDNESS(1013, 35, BlindnessPrayer.class),
  MUSHROOM(1014, 35, MushroomPrayer.class),
  CANNON(1015, 35, CannonPrayer.class),
  MERLIN(1016, 35, MerlinPrayer.class),
  ROCKET(1017, 35, 1, RocketPrayer.class),
  GLASSBOX(1018, 35, GlassBoxPrayer.class),
  DEADLYPOTION(1019, 35, DeadlyPotionPrayer.class),
  NECROSIS(1020, 40, NecrosisPrayer.class),

  FIREBALL(2000, 75, ThrownFireballPrayer.class),
  HEALTH(2001, 15, HealthPrayer.class),
  SPEED(2002, 20, SpeedPrayer.class),
  ANTIFIRE(2003, 15, AntifirePrayer.class),
  POWER(2004, 50, PowerPrayer.class),
  GOD(2005, 150, GodPrayer.class),
  NIGHTVISION(2006, 30, NightVisionPrayer.class),
  FLASH(2007, 40, FlashPrayer.class),
  INVISIBILITY(2008, 30, InvisibilityPrayer.class),
  DEADLYDEFENSE(2009, 75, DeadlyDefensePrayer.class),
  DIGGYDIGGY(2010, 30, DiggyDiggyPrayer.class),
  HULK(2011, 50, HulkPrayer.class),
  HEALTHBOOST(2012, 15, HealthBoostPrayer.class),
  ABSORPTION(2013, 15, AbsorptionPrayer.class);

  private final static Map<Integer, PrayerType> BY_ID = new HashMap<>();

  static {
    for (PrayerType prayerType : values()) {
      BY_ID.put(prayerType.getValue(), prayerType);
    }
  }

  private final int id;
  private final int cost;
  private final long defaultTime;
  private final Class prayerClass;

  PrayerType(int id, int cost, Class prayerClass) {

    this.id = id;
    this.cost = cost;
    this.defaultTime = TimeUnit.MINUTES.toMillis(15);
    this.prayerClass = prayerClass;
  }

  PrayerType(int id, int cost, int defaultTime, Class prayerClass) {

    this.id = id;
    this.cost = cost;
    this.defaultTime = TimeUnit.MINUTES.toMillis(defaultTime);
    this.prayerClass = prayerClass;
  }

  public static PrayerType getId(final int id) {

    return BY_ID.get(id);
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

  public Class getPrayerClass() {

    return prayerClass;
  }

  public boolean isHoly() {

    return getValue() >= 2000;
  }

  public boolean isUnholy() {

    return getValue() < 2000;
  }
}
