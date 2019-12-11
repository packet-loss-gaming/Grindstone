package gg.packetloss.grindstone.util.signwall;

import org.bukkit.entity.Player;

public interface SignWallClickHandler<T> {
    public T handleLeftClick(Player player, T value);
    public default T handleRightClick(Player player, T value) {
        return handleLeftClick(player, value);
    }

    public default boolean allowNavigation() {
        return true;
    }
}
