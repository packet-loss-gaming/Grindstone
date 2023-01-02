/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface RangeWorldMinibossSpawner {
    void spawnBoss(Location spawnLoc, Player target, int worldLevel);
}
