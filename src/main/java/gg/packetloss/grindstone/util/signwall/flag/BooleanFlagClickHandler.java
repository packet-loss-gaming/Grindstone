package gg.packetloss.grindstone.util.signwall.flag;

import gg.packetloss.grindstone.util.flag.BooleanFlagState;
import gg.packetloss.grindstone.util.signwall.SignWallClickHandler;
import org.bukkit.entity.Player;

public class BooleanFlagClickHandler<T extends Enum<T>> implements SignWallClickHandler<BooleanFlagState<T>> {
    @Override
    public BooleanFlagState<T> handleLeftClick(Player player, BooleanFlagState<T> value) {
        value.toggle();
        return value;
    }
}
