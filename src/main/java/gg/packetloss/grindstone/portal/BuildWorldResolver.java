package gg.packetloss.grindstone.portal;

import gg.packetloss.grindstone.firstlogin.FirstLoginComponent;
import gg.packetloss.grindstone.managedworld.ManagedWorldComponent;
import gg.packetloss.grindstone.managedworld.ManagedWorldGetQuery;
import gg.packetloss.grindstone.warps.WarpsComponent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Optional;

public class BuildWorldResolver implements WorldResolver {
    private final ManagedWorldComponent managedWorld;
    private final ManagedWorldGetQuery query;
    private final WarpsComponent warps;
    private final FirstLoginComponent firstLogin;

    public BuildWorldResolver(ManagedWorldComponent managedWorld, ManagedWorldGetQuery query,
                              WarpsComponent warps, FirstLoginComponent firstLogin) {
        this.managedWorld = managedWorld;
        this.query = query;
        this.warps = warps;
        this.firstLogin = firstLogin;
    }

    @Override
    public boolean accepts(Player player) {
        return true;
    }

    private World getWorld() {
        return managedWorld.get(query);
    }

    @Override
    public Optional<Location> getLastExitLocation(Player player) {
        return warps.getLastPortalLocation(player, getWorld());
    }

    @Override
    public Location getDefaultLocationForPlayer(Player player) {
        return firstLogin.getNewPlayerStartingLocation(player);
    }
}
