/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.GraveYard;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class GraveYardConfig extends ConfigurationBase {
    @Setting("apocalypse.chance-of-grave-spawn")
    public int apocalypseChanceOfGraveSpawn = 16;
    @Setting("apocalypse.num-zombies.min")
    public int apocalypseNumZombiesMin = 3;
    @Setting("apocalypse.num-zombies.max")
    public int apocalypseNumZombiesMax = 6;
    @Setting("apocalypse.chance-of-removal")
    public int apocalypseChanceOfPurge = 5;
    @Setting("hostile-temple.spawn-chance")
    public int hostileTempleSpawnChance = 3;
    @Setting("night-spawn.grave-radius")
    public int nightSpawnGraveRadius = 15;
    @Setting("night-spawn.grave-spawn-chance.max")
    public int nightSpawnGraveSpawnChanceMax = 20;
    @Setting("night-spawn.grave-spawn-chance.min")
    public int nightSpawnGraveSpawnChanceMin = 12;
}
