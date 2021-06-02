/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Collection;

public class ScoreboardUtil {
    public static void syncWithPlayers(Scoreboard scoreboard, Collection<Player> players) {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (players.contains(player)) {
                if (scoreboard != player.getScoreboard()) {
                    player.setScoreboard(scoreboard);
                }
            } else {
                if (scoreboard == player.getScoreboard()) {
                    player.setScoreboard(main);
                }
            }
        }
    }
}
