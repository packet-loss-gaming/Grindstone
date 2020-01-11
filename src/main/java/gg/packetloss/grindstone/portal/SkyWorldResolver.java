package gg.packetloss.grindstone.portal;

import gg.packetloss.grindstone.managedworld.ManagedWorldComponent;
import gg.packetloss.grindstone.managedworld.ManagedWorldGetQuery;
import gg.packetloss.grindstone.playerhistory.PlayerHistoryComponent;
import gg.packetloss.grindstone.warps.WarpsComponent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SkyWorldResolver implements WorldResolver {
    private final ManagedWorldComponent managedWorld;
    private final ManagedWorldGetQuery query;
    private final WarpsComponent warps;
    private final PlayerHistoryComponent playerHistory;

    public SkyWorldResolver(ManagedWorldComponent managedWorld, ManagedWorldGetQuery query,
                            WarpsComponent warps, PlayerHistoryComponent playerHistory) {
        this.managedWorld = managedWorld;
        this.query = query;
        this.warps = warps;
        this.playerHistory = playerHistory;
    }

    @Override
    public boolean accepts(Player player) {
        if (player.hasPermission("aurora.severe-offense")) {
            return false;
        }

        if (player.hasPermission("aurora.skyworld.override")) {
            return true;
        }

        try {
            return playerHistory.getTimePlayed(player).get() >= TimeUnit.DAYS.toSeconds(30);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }
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
