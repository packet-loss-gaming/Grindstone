/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.GraveYard;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class GraveYardConfig extends ConfigurationBase {
    @Setting("thunder-storm-cool-down")
    public long tStormCoolDown = 1000 * 60 * 60;
}
