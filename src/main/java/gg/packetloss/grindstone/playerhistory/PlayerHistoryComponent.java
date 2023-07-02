/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.playerhistory;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.data.SQLHandle;
import gg.packetloss.grindstone.world.type.city.CityCoreComponent;
import gg.packetloss.grindstone.world.type.range.RangeCoreComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@ComponentInformation(friendlyName = "Player History", desc = "Player history data.")
@Depend(components = {CityCoreComponent.class, RangeCoreComponent.class})
public class PlayerHistoryComponent extends BukkitComponent implements Listener {
    private Map<UUID, PlayerHistory> playerHistory = new ConcurrentHashMap<>();

    @Override
    public void enable() {
        CommandBook.registerEvents(this);
    }

    private PlayerHistory getHistory(UUID playerID) {
        return playerHistory.compute(playerID, (ignored, existingHistory) -> {
            if (existingHistory == null) {
                existingHistory = new PlayerHistory();
            }

            return existingHistory;
        });
    }

    private void unloadHistory(UUID playerID) {
        PlayerHistory history = playerHistory.get(playerID);
        if (history != null && history.decrement()) {
            playerHistory.remove(playerID);
        }
    }

    private void loadHistory(UUID playerID) {
        PlayerHistory history = getHistory(playerID);

        // History already loaded
        if (history.increment()) {
            return;
        }

        try {
            Optional<Long> optOnlineTime = SQLHandle.getOnlineTime(playerID);
            if (optOnlineTime.isPresent()) {
                history.loadExistingPlayer(optOnlineTime.get());
            } else {
                history.loadNewPlayer();
            }
        } catch (SQLException e) {
            unloadHistory(playerID);
            throw new RuntimeException("History failed to load.", e);
        }
    }

    public CompletableFuture<Long> getTimePlayed(Player player) {
        UUID playerID = player.getUniqueId();
        return getHistory(playerID).getPlayTime();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
        loadHistory(event.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        unloadHistory(event.getPlayer().getUniqueId());
    }
}
