/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.effect.passive;

import gg.packetloss.grindstone.prayer.PassivePrayerEffect;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

public class ZombieSwarmEffect implements PassivePrayerEffect {
    @Override
    public void trigger(Player player) {
        if (player.getWorld().getEntitiesByClass(Zombie.class).size() < 1000) {
            player.getWorld().spawn(player.getLocation(), Zombie.class).setTarget(player);
        }
    }

    @Override
    public void strip(Player player) { }
}
