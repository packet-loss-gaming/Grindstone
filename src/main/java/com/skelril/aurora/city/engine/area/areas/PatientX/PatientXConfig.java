/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.PatientX;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class PatientXConfig extends ConfigurationBase {
    @Setting("boss-health")
    public int bossHealth = 1000;
    @Setting("base-boss-hit")
    public double baseBossHit = 175;
    @Setting("ice-chance")
    public int iceChance = 12;
    @Setting("ice-explosive-change-chance")
    public int iceChangeChance = 30;
    @Setting("snow-ball-chance")
    public int snowBallChance = 10;
    @Setting("player-value")
    public int playerVal = 50000;
    @Setting("default-difficulty")
    public int defaultDifficulty = 3;
    @Setting("min-difficulty")
    public int minDifficulty = 3;
    @Setting("max-difficulty")
    public int maxDifficulty = 9;
}