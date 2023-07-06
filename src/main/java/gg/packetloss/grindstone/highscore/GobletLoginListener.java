/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class GobletLoginListener implements Listener {
  private final HighScoresComponent component;

    public GobletLoginListener(HighScoresComponent component) {
        this.component = component;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        AnnotatedScoreType scoreType = component.getGobletScoreType();
        Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
            if (!player.isOnline()) {
                return;
            }

            ChatUtil.sendNotice(player, "The current goblet is the ", scoreType.getActiveDisplayName(), "!");
            List<ScoreEntry> entries = component.getTop(scoreType, 1);
            if (!entries.isEmpty()) {
                ChatUtil.sendNotice(player, "Lead by the infamous... ", Text.of(ChatColor.DARK_RED, entries.get(0).getPlayer().getName()), "!");
             }
        }, 20 * 2);
    }
}
