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
