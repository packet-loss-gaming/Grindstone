package gg.packetloss.grindstone.util.dropttable;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

class SimpleDrop implements Drop {
    private final Player player;
    private final ItemStack itemStack;

    public SimpleDrop(ItemStack itemStack) {
        this.player = null;
        this.itemStack = itemStack;
    }

    public SimpleDrop(Player player, ItemStack itemStack) {
        this.player = player;
        this.itemStack = itemStack;
    }

    @Override
    public Optional<Player> getPlayer() {
        return Optional.ofNullable(player);
    }

    @Override
    public ItemStack getDrop() {
        return itemStack;
    }
}
