package gg.packetloss.grindstone.util.dropttable;

import org.bukkit.entity.Player;

import java.util.Collection;

public class MassBossKillInfo implements KillInfo {
    private final Collection<Player> players;

    public MassBossKillInfo(Collection<Player> players) {
        this.players = players;
    }

    public Collection<Player> getPlayers() {
        return players;
    }
}
