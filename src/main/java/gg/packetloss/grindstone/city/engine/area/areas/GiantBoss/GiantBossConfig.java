/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.GiantBoss;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

import java.util.List;

public class GiantBossConfig extends ConfigurationBase {
    @Setting("max-health.normal")
    public int maxHealthNormal = 750;
    @Setting("max-health.thunderstorm")
    public int maxHealthThunderstorm = 1000;
    @Setting("babies.max-count")
    public int maxBabies = 200;
    @Setting("babies.min-count")
    public int minBabies = 10;
    @Setting("babies.passive-spawn-chance")
    public int babyPassiveSpawnChance = 100;
    @Setting("babies.max-pot-level")
    public int babyMaxPotLevel = 10;
    @Setting("babies.pot-time")
    public int babyPotTime = 10;
    @Setting("babies.boss-protect-count")
    public int bossProtectBabyCount = 150;
    @Setting("player-throw.min-y-force")
    public double playerThrowMinYForce = .3;
    @Setting("player-throw.max-y-force")
    public double playerThrowMaxYForce = .7;
    @Setting("player-throw.force-amplifier")
    public double playerThrowForceAmplifier = 2;
    @Setting("player-throw.taunts")
    public List<String> playerThrowTaunts = List.of(
            "Weeeeee!", "Poke!", "Boink!", "Hehehehee!",
            "Ha, ha, puny human! This is fun!"
    );
}
