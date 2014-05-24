/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.FreakyFour;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class FreakyFourConfig extends ConfigurationBase {
    @Setting("charlotte-hp")
    public double charlotteHP = 180;
    @Setting("magma-cubed-hp")
    public double magmaCubedHP = 180;
    @Setting("magma-cubed-size")
    public int magmaCubedSize = 8;
    @Setting("magma-cubed-damage-modifier")
    public double magmaCubedDamageModifier = 4;
    @Setting("da-bomb-hp")
    public double daBombHP = 180;
    @Setting("da-bomb-tnt-chance")
    public int daBombTNT = 10;
    @Setting("snipee-hp")
    public double snipeeHP = 180;
    @Setting("snipee-percent-damage")
    public double snipeeDamage = .5;
}
