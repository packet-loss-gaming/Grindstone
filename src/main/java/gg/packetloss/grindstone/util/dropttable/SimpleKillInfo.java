package gg.packetloss.grindstone.util.dropttable;

import org.bukkit.entity.Player;

import java.util.Collection;

public class SimpleKillInfo implements KillInfo {
    private final Collection<Player> players;

    public SimpleKillInfo(Collection<Player> players) {
        this.players = players;
    }

    @Override
    public Collection<Player> getPlayers() {
        return players;
    }
}
