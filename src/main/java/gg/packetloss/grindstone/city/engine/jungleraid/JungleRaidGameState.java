package gg.packetloss.grindstone.city.engine.jungleraid;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class JungleRaidGameState {
    private Map<Player, JungleRaidProfile> playerProfiles = new HashMap<>();

    public boolean hasParticipants() {
        return getPlayers().isEmpty();
    }

    public JungleRaidProfile get(Player player) {
        return playerProfiles.get(player);
    }

    public Collection<Player> getPlayers() {
        return playerProfiles.keySet();
    }

    public Collection<JungleRaidProfile> getProfiles() {
        return playerProfiles.values();
    }

    public void addPlayer(Player player) {
        playerProfiles.put(player, new JungleRaidProfile());
    }

    public void removePlayer(Player player) {
        playerProfiles.remove(player);
    }
}
