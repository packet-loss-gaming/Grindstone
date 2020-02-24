package gg.packetloss.grindstone.util.dropttable;

import org.bukkit.entity.Player;

import java.util.Collection;

public class MassBossKillInfo implements KillInfo {
    private final Collection<Player> players;

    public MassBossKillInfo(Collection<Player> players) {
        this.players = players;
    }

    public int getGlobalChanceModifier() {
        return 1;
    }

    public int getChanceModifier(Player player) {
        return getGlobalChanceModifier();
    }

    public Collection<Player> getPlayers() {
        return players;
    }
}
