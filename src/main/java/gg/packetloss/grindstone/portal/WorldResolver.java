package gg.packetloss.grindstone.portal;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

public interface WorldResolver {
    public boolean accepts(Player player);
    public Optional<Location> getLastExitLocation(Player player);
    public Location getDefaultLocationForPlayer(Player player);
    default public Location getDestinationFor(Player player) {
        Optional<Location> optLastExit = getLastExitLocation(player);
        return optLastExit.orElseGet(() -> getDefaultLocationForPlayer(player));

    }
}
