/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.optimization;

import gg.packetloss.hackbook.entity.HBSimpleZombie;
import org.bukkit.Location;
import org.bukkit.entity.Zombie;

public class OptimizedZombieFactory {
    private OptimizedZombieFactory() { }

    public static Zombie create(Location location) {
        return HBSimpleZombie.spawn(location);
    }
}
