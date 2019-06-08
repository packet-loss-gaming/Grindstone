package gg.packetloss.grindstone.city.engine.pixieitems.manager;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PixieNetworkManager {
    CompletableFuture<Optional<Integer>> createNetwork(UUID namespace, String name);
    CompletableFuture<Optional<Integer>> selectNetwork(UUID namespace, String name);

    CompletableFuture<Optional<NewSourceResult>> addSource(int networkID, Block block);
    CompletableFuture<Optional<NewSinkResult>> addSink(int networkID, Block block);

    boolean maybeExpandChest(Block block);

    boolean removeChest(Location... locations);

    Optional<Integer> getNetworkFromSourceChest(Block... blocks);

    void sourceItems(int networkID, Inventory inventory);

    void handleChunkLoad(Chunk chunk);
    void handleChunkUnload(Chunk chunk);
}