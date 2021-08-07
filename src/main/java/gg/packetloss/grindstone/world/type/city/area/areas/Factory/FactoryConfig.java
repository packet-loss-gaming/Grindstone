/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Factory;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class FactoryConfig extends ConfigurationBase {
    @Setting("production.base-speed")
    public int productionBaseSpeed = 9;
    @Setting("production.stalls.lava-drain-chance")
    public int productionLavaDrainChance = 18;
    @Setting("production.drowned.spawn-chance")
    public int productionDrownedSpawnChance = 5;
    @Setting("production.drowned.trident-chance")
    public int productionDrownedTridentChance = 3;
}
