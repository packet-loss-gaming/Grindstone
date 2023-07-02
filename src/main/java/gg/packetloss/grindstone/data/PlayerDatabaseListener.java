/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.data;

import gg.packetloss.grindstone.util.task.promise.TaskFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

class PlayerDatabaseListener implements Listener {
    private final PlayerDatabase playerDatabase;

    private final HashMap<UUID, Long> playerLoginTime = new HashMap<>();

    public PlayerDatabaseListener(PlayerDatabase playerDatabase) {
        this.playerDatabase = playerDatabase;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
        try {
            playerDatabase.recordPlayerLogin(
                event.getUniqueId(),
                event.getName(),
                event.getAddress().toString()
            );
        } catch (SQLException e) {
            e.printStackTrace();
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            event.kickMessage(Component.text("Login failed, internal error, try again later."));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerID = event.getPlayer().getUniqueId();
        playerLoginTime.put(playerID, System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerID = player.getUniqueId();

        if (playerLoginTime.containsKey(playerID)) {
            long loginTime = playerLoginTime.remove(playerID);
            long secondsOnline = (System.currentTimeMillis() - loginTime) / 1000;

            TaskFuture.asyncTask(() -> {
                playerDatabase.recordPlayerLogout(playerID, secondsOnline);
                return null;
            });
        }
    }
}
