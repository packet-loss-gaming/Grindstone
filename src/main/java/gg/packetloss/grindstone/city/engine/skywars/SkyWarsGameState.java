package gg.packetloss.grindstone.city.engine.skywars;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SkyWarsGameState {
    private Map<Player, SkyWarsProfile> playerProfiles = new HashMap<>();

    public boolean hasParticipants() {
        return !getPlayers().isEmpty();
    }

    public SkyWarsProfile get(Player player) {
        return playerProfiles.get(player);
    }

    public Collection<Player> getPlayers() {
        return playerProfiles.keySet();
    }

    public Collection<SkyWarsProfile> getProfiles() {
        return playerProfiles.values();
    }

    public void addPlayer(Player player) {
        playerProfiles.put(player, new SkyWarsProfile());
    }

    public void removePlayer(Player player) {
        playerProfiles.remove(player);
    }

    public boolean containsPlayer(Player player) {
        return playerProfiles.containsKey(player);
    }
}
