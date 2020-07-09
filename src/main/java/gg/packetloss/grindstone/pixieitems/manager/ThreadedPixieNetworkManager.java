package gg.packetloss.grindstone.pixieitems.manager;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.pixieitems.BrokerTransaction;
import gg.packetloss.grindstone.pixieitems.PixieSinkVariant;
import gg.packetloss.grindstone.pixieitems.TransactionBroker;
import gg.packetloss.grindstone.pixieitems.db.*;
import gg.packetloss.grindstone.pixieitems.db.mysql.MySQLPixieContainerDatabase;
import gg.packetloss.grindstone.pixieitems.db.mysql.MySQLPixieNetworkDatabase;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.RefCountedTracker;
import org.apache.commons.lang.Validate;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.*;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static gg.packetloss.grindstone.util.item.ItemNameCalculator.computeItemName;

public class ThreadedPixieNetworkManager implements PixieNetworkManager {
    private RefCountedTracker<Integer> networkChunkRefCount = new RefCountedTracker<>();
    private Lock networkChunkRefCountLock = new ReentrantLock();

    private Map<Integer, PixieNetworkGraph> idToNetworkMapping = new HashMap<>();
    private Map<Location, Integer> sourceToNetworkMapping = new HashMap<>();
    private ReentrantReadWriteLock networkLock = new ReentrantReadWriteLock();

    private PixieNetworkDatabase networkDatabase = new MySQLPixieNetworkDatabase();
    private PixieContainerDatabase chestDatabase = new MySQLPixieContainerDatabase();

    private NetworkLoadingWorker networkLoadingWorker;

    public ThreadedPixieNetworkManager() {
        networkLoadingWorker = new NetworkLoadingWorker(networkLock);
        networkLoadingWorker.start();
    }

    @Override
    public CompletableFuture<Optional<PixieNetworkDetail>> createNetwork(UUID namespace, String name) {
        CompletableFuture<Optional<PixieNetworkDetail>> future = new CompletableFuture<>();

        CommandBook.server().getScheduler().runTaskAsynchronously(CommandBook.inst(), () -> {
            future.complete(networkDatabase.createNetwork(namespace, name));
        });

        return future;
    }

    @Override
    public CompletableFuture<Optional<PixieNetworkDetail>> selectNetwork(UUID namespace, String name) {
        CompletableFuture<Optional<PixieNetworkDetail>> future = new CompletableFuture<>();

        CommandBook.server().getScheduler().runTaskAsynchronously(CommandBook.inst(), () -> {
            future.complete(networkDatabase.selectNetwork(namespace, name));
        });

        return future;
    }

    @Override
    public CompletableFuture<Optional<PixieNetworkDetail>> selectNetwork(int networkID) {
        CompletableFuture<Optional<PixieNetworkDetail>> future = new CompletableFuture<>();

        CommandBook.server().getScheduler().runTaskAsynchronously(CommandBook.inst(), () -> {
            future.complete(networkDatabase.selectNetwork(networkID));
        });

        return future;
    }

    @Override
    public CompletableFuture<List<PixieNetworkDetail>> selectNetworks(UUID namespace) {
        CompletableFuture<List<PixieNetworkDetail>> future = new CompletableFuture<>();

        CommandBook.server().getScheduler().runTaskAsynchronously(CommandBook.inst(), () -> {
            future.complete(networkDatabase.selectNetworks(namespace));
        });

        return future;
    }

    private List<Location> getLocationsToAdd(Block block) {
        Container chest = (Container) block.getState();
        if (chest.getInventory() instanceof DoubleChestInventory) {
            DoubleChest doubleChest = (DoubleChest) chest.getInventory().getHolder();
            Chest leftChest = (Chest) doubleChest.getLeftSide();
            Chest rightChest = (Chest) doubleChest.getRightSide();

            return Lists.newArrayList(leftChest.getLocation(), rightChest.getLocation());
        } else {
            return Lists.newArrayList(block.getLocation());
        }
    }

    private void clearSinkAtBlock(@Nullable PixieNetworkGraph network, Location location) {
        if (network != null) {
            network.removeSink(location);
        }
    }

    private void clearSourceAtBlock(@Nullable PixieNetworkGraph network, Location location) {
        sourceToNetworkMapping.remove(location);
        if (network != null) {
            network.removeSource(location);
        }
    }

    private void clearBlockMemoryOnly(@Nullable PixieNetworkGraph network, Location location) {
        clearSinkAtBlock(network, location);
        clearSourceAtBlock(network, location);
    }

    private void addSourceMemoryOnly(int networkID, PixieNetworkGraph network, Location location) {
        network.addSource(location);
        sourceToNetworkMapping.put(location, networkID);
    }

    private void updateLoadSet(Collection<Integer> networkIDs, Map<Integer, Boolean> wasPreviouslyLoaded,
                               Set<Integer> loads, Set<Integer> unloads) {
        for (int networkID : networkIDs) {
            boolean wasLoaded = wasPreviouslyLoaded.get(networkID);
            boolean shouldBeLoaded = networkChunkRefCount.contains(networkID);

            if (wasLoaded && !shouldBeLoaded) {
                unloads.add(networkID);
            } else if (!wasLoaded && shouldBeLoaded) {
                loads.add(networkID);
            }
        }
    }

    private <T> CompletableFuture<T> processNetworkChanges(Supplier<T> op, Location... locations) {
        Validate.isTrue(locations.length > 0);

        Set<Chunk> chunks = Arrays.stream(locations).map(Location::getChunk).collect(Collectors.toSet());

        CompletableFuture<T> future = new CompletableFuture<>();

        CommandBook.server().getScheduler().runTaskAsynchronously(CommandBook.inst(), () -> {
            List<Integer> added = new ArrayList<>();
            List<Integer> removed = new ArrayList<>();

            HashMap<Chunk, Collection<Integer>> chunkPreviousNetworks = new HashMap<>();
            HashMap<Chunk, Collection<Integer>> chunkNewNetworks = new HashMap<>();

            for (Chunk chunk : chunks) {
                chunkPreviousNetworks.put(chunk, chestDatabase.getNetworksInChunk(chunk).orElseThrow());
            }

            T returnValue = op.get();

            for (Chunk chunk : chunks) {
                chunkNewNetworks.put(chunk, chestDatabase.getNetworksInChunk(chunk).orElseThrow());
            }

            for (Chunk chunk : chunks) {
                Collection<Integer> previousNetworks = chunkPreviousNetworks.get(chunk);
                Collection<Integer> newNetworks = chunkNewNetworks.get(chunk);

                newNetworks.stream().filter(networkID -> !previousNetworks.contains(networkID)).forEach(added::add);
                previousNetworks.stream().filter(networkID -> !newNetworks.contains(networkID)).forEach(removed::add);
            }

            networkChunkRefCountLock.lock();
            try {
                Map<Integer, Boolean> wasPreviouslyLoaded = new HashMap<>();

                for (int networkID : added) {
                    wasPreviouslyLoaded.putIfAbsent(networkID, networkChunkRefCount.contains(networkID));
                    networkChunkRefCount.increment(networkID);
                }

                for (int networkID : removed) {
                    wasPreviouslyLoaded.putIfAbsent(networkID, networkChunkRefCount.contains(networkID));
                    networkChunkRefCount.decrement(networkID);
                }


                Set<Integer> loads = new HashSet<>();
                Set<Integer> unloads = new HashSet<>();

                updateLoadSet(added, wasPreviouslyLoaded, loads, unloads);
                updateLoadSet(removed, wasPreviouslyLoaded, loads, unloads);

                if (loads.isEmpty()) {
                    future.complete(returnValue);
                } else {
                    loadNetworks(Lists.newArrayList(loads)).thenAccept((ignored) -> future.complete(returnValue));
                }

                if (!unloads.isEmpty()) {
                    unloadNetworks(Lists.newArrayList(unloads));
                }
            } finally {
                networkChunkRefCountLock.unlock();
            }
        });

        return future;
    }

    private boolean isAlreadySourceForNetwork(int networkID, Location... locations) {
        networkLock.readLock().lock();

        try {
            for (Location loc : locations) {
                if (!sourceToNetworkMapping.containsKey(loc) || sourceToNetworkMapping.get(loc) != networkID) {
                    return false;
                }
            }
        } finally {
            networkLock.readLock().unlock();
        }

        return true;
    }

    @Override
    public CompletableFuture<NewSourceResult> addSource(int networkID, Block block) {
        Location[] locations = getLocationsToAdd(block).toArray(new Location[0]);

        // Detect reassigning the block to a source.
        if (isAlreadySourceForNetwork(networkID, locations)) {
            return CompletableFuture.completedFuture(new NewSourceResult(false));
        }

        return processNetworkChanges(() -> {
            networkLock.writeLock().lock();

            try {
                PixieNetworkGraph network = idToNetworkMapping.get(networkID);

                for (Location location : locations) {
                    clearBlockMemoryOnly(network, location);
                    if (network != null) {
                        addSourceMemoryOnly(networkID, network, location);
                    }
                }
            } finally {
                networkLock.writeLock().unlock();
            }

            Optional<Integer> removedChestsCount = chestDatabase.removeContainer(networkID, locations);
            Validate.isTrue(removedChestsCount.isPresent());

            boolean addedChests = chestDatabase.addSource(networkID, locations);
            Validate.isTrue(addedChests);

            return new NewSourceResult(true);
        }, locations);
    }

    private Set<String> extractItemNames(Inventory inventory) {
        Set<String> itemNames = new HashSet<>();

        for (ItemStack itemStack : inventory) {
            computeItemName(itemStack).ifPresent(itemNames::add);
        }

        return itemNames;
    }

    private void addSinkMemoryOnly(PixieNetworkGraph network, Set<String> itemNames, Location location) {
        network.addSink(itemNames, location);
    }

    private CompletableFuture<Void> incrementNetworkLoad(int networkID) {
        networkChunkRefCountLock.lock();
        try {
            if (networkChunkRefCount.increment(networkID)) {
                return loadNetworks(List.of(networkID));
            } else {
                return CompletableFuture.completedFuture(null);
            }
        } finally {
            networkChunkRefCountLock.unlock();
        }
    }

    private void decrementNetworkLoad(int networkID) {
        networkChunkRefCountLock.lock();
        try {
            if (networkChunkRefCount.decrement(networkID)) {
                unloadNetworks(List.of(networkID));
            }
        } finally {
            networkChunkRefCountLock.unlock();
        }
    }

    private <T> CompletableFuture<T> temporarilyLoadIfUnloaded(int networkID, Function<PixieNetworkGraph, CompletableFuture<T>> consumer) {
        return incrementNetworkLoad(networkID).thenApply((ignored) -> {
            networkLock.readLock().lock();

            try {
                return idToNetworkMapping.get(networkID);
            } finally {
                networkLock.readLock().unlock();
            }
        }).thenApply(consumer).thenCompose((result) -> {
            decrementNetworkLoad(networkID);
            return result;
        });
    }

    @Override
    public CompletableFuture<NewSinkResult> addSink(int networkID, Block block, PixieSinkVariant variant) {
        Inventory chestInventory = ((Container) block.getState()).getInventory();
        Set<String> itemNames;
        switch (variant) {
            case VOID:
                itemNames = Set.of();
                break;
            default:
                itemNames = extractItemNames(chestInventory);
                break;
        }

        Location[] locations = getLocationsToAdd(block).toArray(new Location[0]);

        // Since this is a sink chest, it may not "just be loaded".
        return temporarilyLoadIfUnloaded(networkID, (network) -> processNetworkChanges(() -> {
            networkLock.writeLock().lock();

            try {
                for (Location location : locations) {
                    if (variant == PixieSinkVariant.ADD) {
                        itemNames.addAll(network.getSinksAtLocation(location));
                    }

                    clearBlockMemoryOnly(network, location);
                    addSinkMemoryOnly(network, itemNames, location);
                }
            } finally {
                networkLock.writeLock().unlock();
            }

            Optional<Integer> removedChestsCount = chestDatabase.removeContainer(networkID, locations);
            Validate.isTrue(removedChestsCount.isPresent());

            boolean addedChests = chestDatabase.addSink(networkID, itemNames, locations);
            Validate.isTrue(addedChests);

            return new NewSinkResult(removedChestsCount.get(), itemNames);
        }, locations));
    }

    @Override
    public boolean maybeExpandChest(Block block) {
        List<Location> chestLocations = getLocationsToAdd(block);
        chestLocations.remove(block.getLocation());
        if (chestLocations.isEmpty()) {
            return true;
        }

        Validate.isTrue(chestLocations.size() == 1);

        Location existingBlockLoc = chestLocations.get(0);
        Location newBlockLoc = block.getLocation();

        CommandBook.server().getScheduler().runTaskAsynchronously(CommandBook.inst(), () -> {
            Optional<PixieChestDetail> optChestDetail = chestDatabase.getDetailsAtLocation(existingBlockLoc);

            if (optChestDetail.isEmpty()) {
                return;
            }

            PixieChestDetail chestDetail = optChestDetail.get();
            int networkID = chestDetail.getNetworkID();

            networkLock.writeLock().lock();
            try {
                PixieNetworkGraph network = idToNetworkMapping.get(networkID);
                if (network != null) {
                    switch (chestDetail.getChestKind()) {
                        case SOURCE:
                            network.addSource(newBlockLoc);
                            break;
                        case SINK:
                            network.addSink(chestDetail.getSinkItems(), newBlockLoc);
                            break;
                    }
                }
            } finally {
                networkLock.writeLock().unlock();
            }

            switch (chestDetail.getChestKind()) {
                case SOURCE:
                    chestDatabase.addSource(networkID, newBlockLoc);
                    break;
                case SINK:
                    chestDatabase.addSink(networkID, chestDetail.getSinkItems(), newBlockLoc);
                    break;
            }
        });

        return true;
    }

    @Override
    public CompletableFuture<Void> removeContainer(Location... locations) {
        return processNetworkChanges(() -> {
            Collection<Integer> networkIDs = chestDatabase.getNetworksInLocations(locations).get();

            networkLock.writeLock().lock();
            try {
                for (int networkID : networkIDs) {
                    PixieNetworkGraph network = idToNetworkMapping.get(networkID);
                    for (Location loc : locations) {
                        clearBlockMemoryOnly(network, loc);
                    }
                }
            } finally {
                networkLock.writeLock().unlock();
            }

            for (int networkID : networkIDs) {
                chestDatabase.removeContainer(networkID, locations);
            }

            return null;
        }, locations);
    }

    @Override
    public Optional<Integer> getNetworkFromSourceContainers(Block... blocks) {
        networkLock.readLock().lock();

        try {
            for (Block block : blocks) {
                int networkID = sourceToNetworkMapping.getOrDefault(block.getLocation(), -1);
                if (networkID != -1) {
                    return Optional.of(networkID);
                }
            }
        } finally {
            networkLock.readLock().unlock();
        }

        return Optional.empty();
    }

    private static final ParticleBuilder PARTICLE_EFFECT = new ParticleBuilder(Particle.SPELL_INSTANT).count(0).allPlayers();

    private void playEffect(Inventory inv) {
        if (inv instanceof DoubleChestInventory) {
            playEffect(((DoubleChestInventory) inv).getLeftSide());
            playEffect(((DoubleChestInventory) inv).getRightSide());
            return;
        }

        Location baseLoc = inv.getLocation();
        for (int i = 0; i < 16; ++i) {
            Location loc = baseLoc.clone().add(
                    ChanceUtil.getRangedRandom(0, 1.0),
                    1,
                    ChanceUtil.getRangedRandom(0, 1.0)
            );
            PARTICLE_EFFECT.location(loc).offset(0, .3, 0).spawn();
        }
        baseLoc.getWorld().playSound(baseLoc, Sound.ITEM_FIRECHARGE_USE, 1, 1);
    }

    private void playEffect(Inventory from, Inventory to) {
        playEffect(from);
        playEffect(to);
    }

    @Override
    public void sourceItems(TransactionBroker broker, int networkID, Inventory inventory) {
        networkLock.readLock().lock();

        try {
            PixieNetworkGraph network = idToNetworkMapping.get(networkID);

            for (int i = 0; i < inventory.getSize(); ++i) {
                ItemStack item = inventory.getItem(i);

                Optional<String> optItemName = computeItemName(item);
                if (optItemName.isEmpty()) {
                    continue;
                }

                BrokerTransaction authorization = broker.authorizeMovement(item);
                if (!authorization.isAuthorized()) {
                    continue;
                }

                String itemName = optItemName.get();
                for (Location location : network.getSinksForItem(itemName)) {
                    BlockState state = location.getBlock().getState();

                    // If we found an invalid chest, remove it.
                    if (!(state instanceof Container)) {
                        removeContainer(location);
                        continue;
                    }

                    Inventory destInv = ((Container) state).getInventory();

                    playEffect(inventory, destInv);

                    item = destInv.addItem(item).get(0);
                    if (item == null) {
                        // We successfully moved this item, break the inner loop.
                        break;
                    }
                }

                authorization.complete(item);

                inventory.setItem(i, item);
            }
        } finally {
            networkLock.readLock().unlock();
            broker.applyCharges();
        }
    }

    private CompletableFuture<Void> loadNetworks(List<Integer> networkIDs) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        CommandBook.server().getScheduler().runTaskAsynchronously(CommandBook.inst(), () -> {
            Collection<PixieNetworkDefinition> networkDefinitions = chestDatabase.getChestsInNetworks(networkIDs).orElseThrow();

            List<CompletableFuture<Integer>> futures = new ArrayList<>();

            // Add tasks does not create a new thread, so we can safely add a future for each
            // network definition to the list of futures.
            networkLoadingWorker.addTasks((consumer) -> {
                for (PixieNetworkDefinition networkDefinition : networkDefinitions) {
                    // Add the future
                    CompletableFuture<Integer> localFuture = new CompletableFuture<>() ;
                    futures.add(localFuture);

                    // Perform the load operation
                    consumer.accept(() -> {
                        int networkID = networkDefinition.getNetworkID();

                        PixieNetworkGraph network = new PixieNetworkGraph();
                        for (PixieChestDefinition chest : networkDefinition.getChests()) {
                            switch (chest.getChestKind()) {
                                case SOURCE:
                                    addSourceMemoryOnly(networkID, network, chest.getLocation());
                                    break;
                                case SINK:
                                    addSinkMemoryOnly(network, chest.getSinkItems(), chest.getLocation());
                                    break;
                            }
                        }

                        idToNetworkMapping.put(networkID, network);

                        localFuture.complete(networkID);
                    });
                }
            });

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenAccept(future::complete);
        });

        return future;
    }

    private void unloadNetworks(List<Integer> networkIDs) {
        networkLoadingWorker.addTasks((consumer) -> {
            for (int networkID : networkIDs) {
                consumer.accept(() -> {
                    PixieNetworkGraph networkGraph = idToNetworkMapping.remove(networkID);
                    for (Location source : networkGraph.getSources()) {
                        sourceToNetworkMapping.remove(source);
                    }
                });
            }
        });
    }

    @Override
    public void handleChunkLoad(Chunk chunk) {
        CommandBook.server().getScheduler().runTaskAsynchronously(CommandBook.inst(), () -> {
            Collection<Integer> networksInChunk = chestDatabase.getNetworksInChunk(chunk).orElseThrow();

            networkChunkRefCountLock.lock();
            try {
                List<Integer> networksToLoad = new ArrayList<>();

                for (Integer networkID : networksInChunk) {
                    if (networkChunkRefCount.increment(networkID)) {
                        networksToLoad.add(networkID);
                    }
                }

                if (!networksToLoad.isEmpty()) {
                    loadNetworks(networksToLoad);
                }
            } finally {
                networkChunkRefCountLock.unlock();
            }
        });
    }

    @Override
    public void handleChunkUnload(Chunk chunk) {
        CommandBook.server().getScheduler().runTaskAsynchronously(CommandBook.inst(), () -> {
            Collection<Integer> networksInChunk = chestDatabase.getNetworksInChunk(chunk).orElseThrow();

            networkChunkRefCountLock.lock();
            try {
                List<Integer> networksToUnload = new ArrayList<>();

                for (Integer networkID : networksInChunk) {
                    if (networkChunkRefCount.decrement(networkID)) {
                        networksToUnload.add(networkID);
                    }
                }

                if (!networksToUnload.isEmpty()) {
                    unloadNetworks(networksToUnload);
                }
            } finally {
                networkChunkRefCountLock.unlock();
            }
        });
    }
}
