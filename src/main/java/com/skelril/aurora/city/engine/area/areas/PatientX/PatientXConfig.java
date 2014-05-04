/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.PatientX;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class PatientXConfig extends ConfigurationBase {
    @Setting("base-health")
    public int baseHealth = 700;
    @Setting("ice-chance")
    public int iceChance = 12;
}