package gg.packetloss.grindstone.helper;

import org.bukkit.entity.Player;

import java.util.Collection;

public interface Response {
    boolean accept(Player player, Collection<Player> recipients, String string);

    default String getNamePlate() {
        return "[Jeeves]";
    }
}
