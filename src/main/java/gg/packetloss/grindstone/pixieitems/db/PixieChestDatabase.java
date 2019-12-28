package gg.packetloss.grindstone.pixieitems.db;

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PixieChestDatabase {
    boolean addSource(int networkID, Location... locations);
    boolean addSink(int networkID, Set<String> itemNames, Location... locations);

    Optional<Integer> removeChest(int networkID, Location... locations);

    Optional<PixieChestDetail> getDetailsAtLocation(Location location);
    Optional<Collection<Integer>> getNetworksInLocations(Location... locations);

    Optional<Collection<Integer>> getNetworksInChunk(Chunk chunk);
    Optional<Collection<PixieNetworkDefinition>> getChestsInNetworks(List<Integer> networks);
}
