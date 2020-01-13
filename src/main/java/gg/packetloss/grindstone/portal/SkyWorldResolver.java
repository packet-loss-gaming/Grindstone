package gg.packetloss.grindstone.portal;

import gg.packetloss.grindstone.city.engine.SkyWorldCoreComponent;
import gg.packetloss.grindstone.managedworld.ManagedWorldComponent;
import gg.packetloss.grindstone.managedworld.ManagedWorldGetQuery;
import gg.packetloss.grindstone.warps.WarpsComponent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Optional;

public class SkyWorldResolver implements WorldResolver {
    private final ManagedWorldComponent managedWorld;
    private final ManagedWorldGetQuery query;
    private final WarpsComponent warps;
    private final SkyWorldCoreComponent skyWorldCore;

    public SkyWorldResolver(ManagedWorldComponent managedWorld, ManagedWorldGetQuery query,
                            WarpsComponent warps, SkyWorldCoreComponent skyWorldCore) {
        this.managedWorld = managedWorld;
        this.query = query;
        this.warps = warps;
        this.skyWorldCore = skyWorldCore;
    }

    @Override
    public boolean accepts(Player player) {
        return skyWorldCore.hasAccess(player);
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
        return getWorld().getSpawnLocation();
    }
}
