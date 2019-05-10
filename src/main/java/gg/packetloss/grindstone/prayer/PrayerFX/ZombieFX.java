/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.PrayerFX;

import gg.packetloss.grindstone.prayer.PrayerType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

public class ZombieFX extends AbstractEffect {

    @Override
    public PrayerType getType() {

        return PrayerType.ZOMBIE;
    }

    @Override
    public void add(Player player) {

        if (player.getWorld().getEntitiesByClass(Zombie.class).size() < 1000) {
            player.getWorld().spawn(player.getLocation(), Zombie.class).setTarget(player);
        }
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
