package gg.packetloss.grindstone.util.dropttable;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public interface Drop {
    public Optional<Player> getPlayer();
    public ItemStack getDrop();
}
