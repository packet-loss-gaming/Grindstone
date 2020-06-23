package gg.packetloss.grindstone.pixieitems.manager;

import gg.packetloss.grindstone.pixieitems.PixieSinkVariant;
import gg.packetloss.grindstone.pixieitems.TransactionBroker;
import gg.packetloss.grindstone.pixieitems.db.PixieNetworkDetail;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PixieNetworkManager {
    CompletableFuture<Optional<PixieNetworkDetail>> createNetwork(UUID namespace, String name);
    CompletableFuture<Optional<PixieNetworkDetail>> selectNetwork(UUID namespace, String name);
    CompletableFuture<Optional<PixieNetworkDetail>> selectNetwork(int networkID);
    CompletableFuture<List<PixieNetworkDetail>> selectNetworks(UUID namespace);

    CompletableFuture<NewSourceResult> addSource(int networkID, Block block);
    CompletableFuture<NewSinkResult> addSink(int networkID, Block block, PixieSinkVariant variant);

    boolean maybeExpandChest(Block block);

    CompletableFuture<Void> removeContainer(Location... locations);

    Optional<Integer> getNetworkFromSourceContainers(Block... blocks);

    void sourceItems(TransactionBroker broker, int networkID, Inventory inventory);

    void handleChunkLoad(Chunk chunk);
    void handleChunkUnload(Chunk chunk);
}
