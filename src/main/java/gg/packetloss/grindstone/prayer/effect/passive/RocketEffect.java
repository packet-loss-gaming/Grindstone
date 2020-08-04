/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.effect.passive;

import gg.packetloss.grindstone.prayer.PassivePrayerEffect;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class RocketEffect implements PassivePrayerEffect {
    @Override
    public void trigger(Player player) {
        Location playerLoc = player.getLocation();
        if (playerLoc.getY() - 4 < player.getWorld().getHighestBlockYAt(playerLoc.getBlockX(), playerLoc.getBlockZ())) {
            player.setVelocity(new Vector(0, 4, 0));
        }
    }

    @Override
    public void strip(Player player) { }
}
