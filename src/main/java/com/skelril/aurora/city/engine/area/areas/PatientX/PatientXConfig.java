/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.PatientX;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class PatientXConfig extends ConfigurationBase {
    @Setting("boss.health")
    public int bossHealth = 1000;
    @Setting("boss.base-hit")
    public double baseBossHit = 1.7;
    @Setting("ice.chance")
    public int iceChance = 12;
    @Setting("ice.explosive-chance")
    public int iceChangeChance = 30;
    @Setting("radiation.light-level")
    public int radiationLightLevel = 8;
    @Setting("radiation.times")
    public int radiationTimes = 40;
    @Setting("radiation.multiplier")
    public double radiationMultiplier = 3;
    @Setting("snow-ball-chance")
    public int snowBallChance = 10;
    @Setting("player-value")
    public int playerVal = 50000;
    @Setting("difficulty.default")
    public int defaultDifficulty = 3;
    @Setting("difficulty.min")
    public int minDifficulty = 3;
    @Setting("difficulty.max")
    public int maxDifficulty = 9;
}