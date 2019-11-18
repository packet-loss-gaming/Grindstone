package gg.packetloss.grindstone.helper;

import gg.packetloss.bukkittext.Text;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;

public interface Response {
    boolean accept(Player player, Collection<Player> recipients, String string);

    default Text getNamePlate() {
        return Text.of(
                ChatColor.WHITE, "<",
                ChatColor.BLACK, "[", ChatColor.RED, "Bot", ChatColor.BLACK, "]",
                ChatColor.WHITE, " Jeeves",
                ChatColor.WHITE, "> "
        );
    }
}
