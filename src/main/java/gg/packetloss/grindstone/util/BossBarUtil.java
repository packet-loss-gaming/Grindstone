/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

public class BossBarUtil {
    public static void syncWithPlayers(BossBar bar, Collection<Player> players) {
        List<Player> currentPlayers = bar.getPlayers();
        for (Player player : currentPlayers) {
            if (players.contains(player)) {
                continue;
            }

            bar.removePlayer(player);
        }

        for (Player player : players) {
            bar.addPlayer(player);
        }
    }
}
