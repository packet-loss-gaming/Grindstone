package gg.packetloss.grindstone.util.dropttable;

import org.bukkit.entity.Player;

import java.util.Collection;

public interface KillInfo {
    default public int getGlobalChanceModifier() {
        return 1;
    }

    default public int getChanceModifier(Player player) {
        return getGlobalChanceModifier();
    }

    public Collection<Player> getPlayers();
}
