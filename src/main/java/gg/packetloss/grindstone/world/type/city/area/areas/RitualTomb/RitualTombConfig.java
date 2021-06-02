/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.RitualTomb;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class RitualTombConfig extends ConfigurationBase {
    @Setting("ritual.level.interval")
    public double ritualLevelInterval = 2500;
    @Setting("ritual.level.demon-multiplier")
    public double ritualLevelDemonMultiplier = 1.5;
    @Setting("ritual.level.floor-removal")
    public int ritualLevelFloorRemoval = 5;
    @Setting("ritual.demons.count.min")
    public int ritualDemonsCountMin = 5;
    @Setting("ritual.demons.count.max")
    public int ritualDemonsCountMax = 50;
    @Setting("ritual.demons.starting-amount")
    public int ritualDemonsStartingAmount = 200;
}
