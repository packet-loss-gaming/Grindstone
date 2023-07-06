/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.player;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.RefCountedTracker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class FallDamageDeathBlocker {
    private static boolean isLaunched(Player player) {
        Location belowLoc = player.getLocation().add(0, -1, 0);
        return EnvironmentUtil.isAirBlock(belowLoc.getBlock()) && player.getVelocity().getY() > 0;
    }

    public static void protectPlayer(Player player, RefCountedTracker<UUID> tracker) {
        Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
            if (!isLaunched(player)) {
                return;
            }

            tracker.increment(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
                tracker.decrement(player.getUniqueId());
            }, 20 * 20);
        }, 20);
    }

}
