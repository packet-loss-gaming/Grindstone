/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.FreakyFour;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class FreakyFourConfig extends ConfigurationBase {
    @Setting("charlotte.hp")
    public double charlotteHP = 180;
    @Setting("charlotte.web-break-chance")
    public int charlotteWebBreak = 3;
    @Setting("charlotte.web-to-spider-chance")
    public int charlotteWebSpider = 15;
    @Setting("charlotte.healing-scale")
    public double charlotteHealingScale = 1.5;
    @Setting("frimus.hp")
    public double frimusHP = 900;
    @Setting("frimus.wall-density")
    public int frimusWallDensity = 3;
    @Setting("da-bomb.hp")
    public double daBombHP = 180;
    @Setting("da-bomb.teleport-min-dist")
    public int daBombTeleMinDist = 10;
    @Setting("snipee.hp")
    public double snipeeHP = 180;
    @Setting("snipee.percent-damage")
    public double snipeeDamage = .5;
}
