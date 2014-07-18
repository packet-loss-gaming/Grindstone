/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.MirageArena;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class MirageArenaConfig extends ConfigurationBase {
    @Setting("fake-xp-amount")
    public int fakeXP = 100;
    @Setting("gold.cap")
    public int goldCap = 200;
    @Setting("gold.bar-chance")
    public int goldBarChance = 27;
}
