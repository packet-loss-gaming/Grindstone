package gg.packetloss.grindstone.playerhistory;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.data.MySQLHandle;
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

    public CompletableFuture<Long> getTimePlayed(Player player) {
        UUID playerID = player.getUniqueId();
        return getHistory(playerID).getPlayTime();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
        UUID playerID = event.getUniqueId();
        try {
            Optional<Long> optOnlineTime = MySQLHandle.getOnlineTime(playerID);
            if (optOnlineTime.isPresent()) {
                getHistory(playerID).loadExistingPlayer(optOnlineTime.get());
            } else {
                getHistory(playerID).loadNewPlayer();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerHistory.remove(event.getPlayer().getUniqueId());
    }
}
