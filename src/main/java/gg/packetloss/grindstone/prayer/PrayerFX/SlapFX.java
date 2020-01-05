/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.PrayerFX;

import gg.packetloss.grindstone.prayer.PrayerType;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Random;

public class SlapFX extends AbstractEffect {

    private final Random random = new Random();

    public SlapFX() {
    }

    @Override
    public PrayerType getType() {

        return PrayerType.SLAP;
    }

    @Override
    public void add(Player player) {

        super.add(player);
        Location playerLoc = player.getLocation();
        if (playerLoc.getY() - 4 < player.getWorld().getHighestBlockYAt(playerLoc.getBlockX(), playerLoc.getBlockZ())) {
            player.setVelocity(new Vector(
                    random.nextDouble() * 5.0 - 2.5,
                    random.nextDouble() * 4,
                    random.nextDouble() * 5.0 - 2.5));
        }

        EntityUtil.forceDamage(player, ChanceUtil.getRandom(3));
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
