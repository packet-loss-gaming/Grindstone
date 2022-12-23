/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Bessi;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class BessiConfig extends ConfigurationBase {
    @Setting("cows.dead-threshold")
    public int numDeadCowsRequired = 30;
    @Setting("cows.desired-count")
    public int numCowsDesired = 100;
    @Setting("cows.spawned-per-tick")
    public int numCowsPerTick = 5;
    @Setting("bessi.health")
    public double bessiHealth = 1000;
    @Setting("bessi.speed")
    public double bessiSpeed = .4;
    @Setting("bessi.allies-spawned.max")
    public int bessiAlliesSpawnedMax = 5;
    @Setting("bessi.allies-spawned.min")
    public int bessiAlliesSpawnedMin = 1;
    @Setting("bessi.corruption-blocks")
    public int bessiCorruptionBlocks = 5;
    @Setting("bessi.corruption-interval-ticks")
    public int bessiCorruptionIntervalTicks= 10;
    @Setting("bessi.corruption-phases")
    public int bessiCorruptionPhases = 20;
    @Setting("bessi.fireball-interval-ticks")
    public int bessiFireballIntervalTicks= 10;
    @Setting("bessi.fireballs-fired")
    public int bessiFireballsFired = 10;
    @Setting("bessi.attack-chance")
    public int bessiAttackChance = 7;
}
