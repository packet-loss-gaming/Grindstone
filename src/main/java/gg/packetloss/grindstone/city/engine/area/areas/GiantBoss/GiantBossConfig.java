/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.GiantBoss;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class GiantBossConfig extends ConfigurationBase {
    @Setting("max-health.normal")
    public int maxHealthNormal = 750;
    @Setting("max-health.thunderstorm")
    public int maxHealthThunderstorm = 1000;
    @Setting("babies.max-count")
    public int maxBabies = 200;
    @Setting("babies.max-pot-level")
    public int babyMaxPotLevel = 10;
    @Setting("babies.pot-time")
    public int babyPotTime = 10;
    @Setting("babies.boss-protect-count")
    public int bossProtectBabyCount = 150;
}
