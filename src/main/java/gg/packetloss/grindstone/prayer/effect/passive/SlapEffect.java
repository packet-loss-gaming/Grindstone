/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.effect.passive;

import gg.packetloss.grindstone.prayer.PassivePrayerEffect;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class SlapEffect implements PassivePrayerEffect {
    @Override
    public void trigger(Player player) {
        Location playerLoc = player.getLocation();
        if (playerLoc.getY() - 4 < player.getWorld().getHighestBlockYAt(playerLoc.getBlockX(), playerLoc.getBlockZ())) {
            ThreadLocalRandom current = ThreadLocalRandom.current();
            player.setVelocity(new Vector(
                current.nextDouble() * 5.0 - 2.5,
                current.nextDouble() * 4,
                current.nextDouble() * 5.0 - 2.5
            ));
        }

        EntityUtil.forceDamage(player, ChanceUtil.getRandom(3));
    }

    @Override
    public void strip(Player player) { }
}
