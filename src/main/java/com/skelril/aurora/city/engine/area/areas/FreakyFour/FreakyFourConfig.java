/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.FreakyFour;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class FreakyFourConfig extends ConfigurationBase {
    @Setting("fake-xp-amount")
    public int fakeXP = 100;
    @Setting("minimum-loot")
    public double minLoot = 50000;
    @Setting("bank-percent")
    public double bankPercent = .12;
    @Setting("teleport-behind-chance")
    public int backTeleport = 3;
    @Setting("charlotte.hp")
    public double charlotteHP = 180;
    @Setting("charlotte.floor-web-chance")
    public int charlotteFloorWeb = 15;
    @Setting("charlotte.web-to-spider-chance")
    public int charlotteWebSpider = 15;
    @Setting("magma-cubed.hp")
    public double magmaCubedHP = 180;
    @Setting("magma-cubed.size")
    public int magmaCubedSize = 8;
    @Setting("magma-cubed.damage-modifier")
    public double magmaCubedDamageModifier = 4;
    @Setting("da-bomb.hp")
    public double daBombHP = 180;
    @Setting("da-bomb.tnt-chance")
    public int daBombTNT = 10;
    @Setting("da-bomb.tnt-strength")
    public int daBombTNTStrength = 8;
    @Setting("snipee.hp")
    public double snipeeHP = 180;
    @Setting("snipee.teleport-distance")
    public double snipeeTeleportDist = 10;
    @Setting("snipee.percent-damage")
    public double snipeeDamage = .5;
}
