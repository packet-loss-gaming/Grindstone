/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer;

import gg.packetloss.grindstone.prayer.effect.interactive.ThrownFireballEffect;
import gg.packetloss.grindstone.prayer.effect.passive.*;

public class PrayerEffects {
    // Interactive
    public static ThrownFireballEffect THROWN_FIREBALL = new ThrownFireballEffect();

    // Passive
    public static ArrowEffect ARROW = new ArrowEffect();
    public static ButterFingersEffect BUTTER_FINGERS = new ButterFingersEffect();
    public static CannonEffect CANNON = new CannonEffect();
    public static DeadlyDefenseEffect DEADLY_DEFENSE = new DeadlyDefenseEffect();
    public static DeadlyPotionEffect DEADLY_POTION = new DeadlyPotionEffect();
    public static FireEffect FIRE = new FireEffect();
    public static GlassBoxFX GLASS_BOX = new GlassBoxFX();
    public static InfiniteFoodEffect INFINITE_FOOD = new InfiniteFoodEffect();
    public static InventoryEffect INVENTORY = new InventoryEffect();
    public static RacketEffect RACKET = new RacketEffect();
    public static RocketEffect ROCKET = new RocketEffect();
    public static SlapEffect SLAP = new SlapEffect();
    public static SmokeEffect SMOKE = new SmokeEffect();
    public static StarvationEffect STARVATION = new StarvationEffect();
    public static TNTEffect TNT = new TNTEffect();
    public static FakeTNTEffect TNT_FAKE = new FakeTNTEffect();
    public static ZombieSwarmEffect ZOMBIE_SWARM = new ZombieSwarmEffect();
}
