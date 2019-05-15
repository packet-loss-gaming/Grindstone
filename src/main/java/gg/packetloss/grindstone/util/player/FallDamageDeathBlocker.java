/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.player;

import com.sk89q.commandbook.CommandBook;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.UUID;

public class FallDamageDeathBlocker {
    private static boolean isLaunched(Player player) {
        return player.getLocation().add(0, -1, 0).getBlock().getType() == Material.AIR && player.getVelocity().getY() > 0;
    }

    public static void protectPlayer(Player player, RefCountedTracker<UUID> tracker) {
        CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
            if (!isLaunched(player)) {
                return;
            }

            tracker.increment(player.getUniqueId());
            CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
                tracker.decrement(player.getUniqueId());
            }, 20 * 20);
        }, 20 * 2);
    }

}