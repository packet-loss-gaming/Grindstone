/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems.manager;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.collect.Lists;
import gg.packetloss.grindstone.pixieitems.BrokerTransaction;
import gg.packetloss.grindstone.pixieitems.PixieSinkCreationMode;
import gg.packetloss.grindstone.pixieitems.TransactionBroker;
import gg.packetloss.grindstone.pixieitems.db.*;
import gg.packetloss.grindstone.pixieitems.db.mysql.MySQLPixieContainerDatabase;
import gg.packetloss.grindstone.pixieitems.db.mysql.MySQLPixieNetworkDatabase;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.PluginTaskExecutor;
import gg.packetloss.grindstone.util.RefCountedTracker;
import gg.packetloss.grindstone.util.item.inventory.InventoryAdapter;
import gg.packetloss.grindstone.util.task.promise.TaskFuture;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
    public TaskFuture<Optional<PixieNetworkDetail>> createNetwork(UUID namespace, String name, Location origin) {
        return TaskFuture.asyncTask(() -> networkDatabase.createNetwork(namespace, name, origin));
    }

    @Override
    public TaskFuture<Optional<PixieNetworkDetail>> selectNetwork(UUID namespace, String name) {
        return TaskFuture.asyncTask(() -> networkDatabase.selectNetwork(namespace, name));
    }

    @Override
    public TaskFuture<Optional<PixieNetworkDetail>> selectNetwork(int networkID) {
        return TaskFuture.asyncTask(() -> networkDatabase.selectNetwork(networkID));
    }

    @Override
    public TaskFuture<List<PixieNetworkDetail>> selectNetworks(UUID namespace) {
        return TaskFuture.asyncTask(() -> networkDatabase.selectNetworks(namespace));
    }

    private List<Location> getLocationsToAdd(Block block) {
        BlockState state = block.getState();
        if (state instanceof Container container) {
            if (container.getInventory() instanceof DoubleChestInventory) {
                DoubleChest doubleChest = (DoubleChest) container.getInventory().getHolder();
                Chest leftChest = (Chest) doubleChest.getLeftSide();
                Chest rightChest = (Chest) doubleChest.getRightSide();

                return Lists.newArrayList(leftChest.getLocation(), rightChest.getLocation());
            }
        }
        return Lists.newArrayList(block.getLocation());
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

    private <T> TaskFuture<T> processNetworkChanges(Supplier<T> op, Location... locations) {
        Validate.isTrue(locations.length > 0);

        Set<Chunk> chunks = Arrays.stream(locations).map(Location::getChunk).collect(Collectors.toSet());

        return TaskFuture.asyncTask(() -> {
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

                try {
                    // If there's something to load, wait for it to load.
                    if (!loads.isEmpty()) {
                        loadNetworks(Lists.newArrayList(loads)).get();
                    }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    if (!unloads.isEmpty()) {
                        unloadNetworks(Lists.newArrayList(unloads));
                    }
                }
                return returnValue;
            } finally {
                networkChunkRefCountLock.unlock();
            }
        });
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
    public TaskFuture<NewSourceResult> addSource(int networkID, Block block) {
        Location[] locations = getLocationsToAdd(block).toArray(new Location[0]);

        // Detect reassigning the block to a source.
        if (isAlreadySourceForNetwork(networkID, locations)) {
            return TaskFuture.completed(new NewSourceResult(false));
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

    private Map<String, List<Integer>> createItemMapping(Inventory inventory, boolean recordPosition) {
        Map<String, List<Integer>> itemNames = new HashMap<>();

        for (int i = 0; i < inventory.getSize(); ++i) {
            Optional<String> optItemName = computeItemName(inventory.getItem(i));
            if (optItemName.isEmpty()) {
                continue;
            }

            List<Integer> positions = itemNames.computeIfAbsent(optItemName.get(), (ignored) -> new ArrayList<>());
            if (recordPosition) {
                positions.add(i);
            }
        }

        return itemNames;
    }

    private void addSinkMemoryOnly(PixieNetworkGraph network, Map<String, List<Integer>> itemMapping, Location location) {
        network.addSink(itemMapping, location);
    }

    private TaskFuture<Void> incrementNetworkLoad(int networkID) {
        networkChunkRefCountLock.lock();
        try {
            if (networkChunkRefCount.increment(networkID)) {
                return loadNetworks(List.of(networkID));
            } else {
                return TaskFuture.completed(null);
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

    private <T> TaskFuture<T> temporarilyLoadIfUnloaded(int networkID, Function<PixieNetworkGraph, TaskFuture<T>> consumer) {
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

    private Inventory getInventoryAtBlock(OfflinePlayer player, BlockState state) {
        if (state instanceof EnderChest) {
            // The player isn't online, this can't be considered at the moment
            if (!player.isOnline()) {
                return null;
            }

            return new RelocatedInventory(
                Objects.requireNonNull(player.getPlayer()).getEnderChest(),
                state.getLocation()
            );
        } else if (state instanceof Container container) {
            return container.getInventory();
        }

        throw new IllegalStateException();
    }

    @Override
    public TaskFuture<NewSinkResult> addSink(OfflinePlayer player, int networkID, Block block,
                                             PixieSinkCreationMode variant) {
        Inventory chestInventory = Objects.requireNonNull(getInventoryAtBlock(player, block.getState()));
        Map<String, List<Integer>> itemMapping = switch (variant) {
            case VOID -> Map.of();
            case ADD, OVERWRITE -> createItemMapping(chestInventory, false);
            case TEMPLATE -> createItemMapping(chestInventory, true);
        };

        Location[] locations = getLocationsToAdd(block).toArray(new Location[0]);

        // Since this is a sink chest, it may not "just be loaded".
        return temporarilyLoadIfUnloaded(networkID, (network) -> processNetworkChanges(() -> {
            networkLock.writeLock().lock();

            try {
                for (Location location : locations) {
                    if (variant == PixieSinkCreationMode.ADD) {
                        for (String itemName : network.getSinksAtLocation(location)) {
                            itemMapping.computeIfAbsent(itemName, (ignored) -> new ArrayList<>());
                        }
                    }

                    clearBlockMemoryOnly(network, location);
                    addSinkMemoryOnly(network, itemMapping, location);
                }
            } finally {
                networkLock.writeLock().unlock();
            }

            Optional<Integer> removedChestsCount = chestDatabase.removeContainer(networkID, locations);
            Validate.isTrue(removedChestsCount.isPresent());

            boolean addedChests = chestDatabase.addSink(networkID, itemMapping, locations);
            Validate.isTrue(addedChests);

            return new NewSinkResult(removedChestsCount.get(), itemMapping.keySet());
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

        PluginTaskExecutor.submitAsync(() -> {
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
                        case SOURCE -> network.addSource(newBlockLoc);
                        case SINK -> network.addSink(chestDetail.getItemMapping(), newBlockLoc);
                    }
                }
            } finally {
                networkLock.writeLock().unlock();
            }

            switch (chestDetail.getChestKind()) {
                case SOURCE -> chestDatabase.addSource(networkID, newBlockLoc);
                case SINK -> chestDatabase.addSink(networkID, chestDetail.getItemMapping(), newBlockLoc);
            }
        });

        return true;
    }

    @Override
    public TaskFuture<Void> removeContainer(Location... locations) {
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
    public void sourceItems(OfflinePlayer player, TransactionBroker broker, int networkID, Inventory inventory) {
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
                for (PixieSink sink : network.getSinksForItem(itemName)) {
                    BlockState state = sink.getBlock().getState();

                    Inventory destInv;
                    try {
                        destInv = getInventoryAtBlock(player, state);
                        if (destInv == null) {
                            continue;
                        }
                    } catch (IllegalStateException ignored) {
                        // If we found an invalid chest, remove it.
                        removeContainer(sink.getLocation());
                        continue;
                    }

                    InventoryAdapter adapter = sink.adaptInventory(destInv);
                    item = adapter.add(Objects.requireNonNull(item));
                    if (adapter.applyChanges()) {
                        playEffect(inventory, destInv);
                    }

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

    private TaskFuture<Void> loadNetworks(List<Integer> networkIDs) {
        TaskFuture<Void> future = new TaskFuture<>();

        PluginTaskExecutor.submitAsync(() -> {
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
                                case SOURCE -> addSourceMemoryOnly(networkID, network, chest.getLocation());
                                case SINK -> addSinkMemoryOnly(network, chest.getItemMapping(), chest.getLocation());
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
        networkLoadingWorker.addTasks((consumer) -> {
            consumer.accept(() -> {
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
        });
    }

    @Override
    public void handleChunkUnload(Chunk chunk) {
        networkLoadingWorker.addTasks((consumer) -> {
            consumer.accept(() -> {
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
        });
    }
}
