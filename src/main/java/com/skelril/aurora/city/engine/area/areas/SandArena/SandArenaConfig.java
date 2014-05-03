/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.SandArena;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class SandArenaConfig extends ConfigurationBase {
    @Setting("increase-rate")
    public int increaseRate = 8;
    @Setting("decrease-rate")
    public int decreaseRate = 16;
}
