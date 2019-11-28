package gg.packetloss.grindstone.city.engine.pixieitems.manager;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.city.engine.pixieitems.BrokerTransaction;
import gg.packetloss.grindstone.city.engine.pixieitems.PixieSinkVariant;
import gg.packetloss.grindstone.city.engine.pixieitems.TransactionBroker;
import gg.packetloss.grindstone.city.engine.pixieitems.db.*;
import gg.packetloss.grindstone.city.engine.pixieitems.db.mysql.MySQLPixieChestDatabase;
import gg.packetloss.grindstone.city.engine.pixieitems.db.mysql.MySQLPixieNetworkDatabase;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.RefCountedTracker;
import org.apache.commons.lang3.Validate;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
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
import java.util.stream.Collectors;

import static gg.packetloss.grindstone.util.item.ItemNameCalculator.computeItemName;

public class ThreadedPixieNetworkManager implements PixieNetworkManager {
    private RefCountedTracker<Integer> networkChunkRefCount = new RefCountedTracker<>();
    private Lock networkChunkRefCountLock = new ReentrantLock();

    private Map<Integer, PixieNetworkGraph> idToNetworkMapping = new HashMap<>();
    private Map<Location, Integer> sourceToNetworkMapping = new HashMap<>();
    private ReentrantReadWriteLock networkLock = new ReentrantReadWriteLock();

    private PixieNetworkDatabase networkDatabase = new MySQLPixieNetworkDatabase();
    private PixieChestDatabase chestDatabase = new MySQLPixieChestDatabase();

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
        Chest chest = (Chest) block.getState();
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

    private CompletableFuture<Boolean> processNetworkChanges(Runnable op, Location... locations) {
        Validate.isTrue(locations.length > 0);

        Set<Chunk> chunks = Arrays.stream(locations).map(Location::getChunk).collect(Collectors.toSet());

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        CommandBook.server().getScheduler().runTaskAsynchronously(CommandBook.inst(), () -> {
            List<Integer> added = new ArrayList<>();
            List<Integer> removed = new ArrayList<>();

            HashMap<Chunk, Collection<Integer>> chunkPreviousNetworks = new HashMap<>();
            HashMap<Chunk, Collection<Integer>> chunkNewNetworks = new HashMap<>();

            for (Chunk chunk : chunks) {
                chunkPreviousNetworks.put(chunk, chestDatabase.getNetworksInChunk(chunk).get());
            }

            op.run();

            for (Chunk chunk : chunks) {
                chunkNewNetworks.put(chunk, chestDatabase.getNetworksInChunk(chunk).get());
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
                    future.complete(true);
                } else {
                    loadNetworks(Lists.newArrayList(loads)).thenAccept(future::complete);
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

    private boolean hasAllSourceLocations(Location... locations) {
        for (Location loc : locations) {
            if (!sourceToNetworkMapping.containsKey(loc)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public CompletableFuture<Optional<NewSourceResult>> addSource(int networkID, Block block) {
        Location[] locations = getLocationsToAdd(block).toArray(new Location[0]);

        if (hasAllSourceLocations(locations)) {
            return CompletableFuture.completedFuture(Optional.of(new NewSourceResult(false)));
        }

        CompletableFuture<Optional<NewSourceResult>> future = new CompletableFuture<>();

        CompletableFuture<Boolean> networkLoadingFuture = new CompletableFuture<>();

        processNetworkChanges(() -> {
            try {
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

                Optional<Integer> removedChestsCount = chestDatabase.removeChest(networkID, locations);
                Validate.isTrue(removedChestsCount.isPresent());

                boolean addedChests = chestDatabase.addSource(networkID, locations);
                Validate.isTrue(addedChests);

                networkLoadingFuture.thenAccept((result) -> {
                    if (result) {
                        // Successfully loaded, return the result.
                        future.complete(Optional.of(new NewSourceResult(true)));
                    } else {
                        // Failed loading.
                        future.complete(Optional.empty());
                    }
                });
            } catch (Throwable t) {
                future.complete(Optional.empty());
                throw t;
            }
        }, locations).thenAccept(networkLoadingFuture::complete);

        return future;
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

    @Override
    public CompletableFuture<Optional<NewSinkResult>> addSink(int networkID, Block block, PixieSinkVariant variant) {
        Inventory chestInventory = ((Chest) block.getState()).getInventory();
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

        CompletableFuture<Optional<NewSinkResult>> future = new CompletableFuture<>();

        processNetworkChanges(() -> {
            try {
                // Network may be unloaded for a sink chest
                networkLock.writeLock().lock();

                try {
                    PixieNetworkGraph network = idToNetworkMapping.get(networkID);

                    for (Location location : locations) {
                        if (variant == PixieSinkVariant.ADD) {
                            itemNames.addAll(network.getSinksAtLocation(location));
                        }

                        clearBlockMemoryOnly(network, location);
                        if (network != null) {
                            addSinkMemoryOnly(network, itemNames, location);
                        }
                    }
                } finally {
                    networkLock.writeLock().unlock();
                }

                Optional<Integer> removedChestsCount = chestDatabase.removeChest(networkID, locations);
                Validate.isTrue(removedChestsCount.isPresent());

                boolean addedChests = chestDatabase.addSink(networkID, itemNames, locations);
                Validate.isTrue(addedChests);

                future.complete(Optional.of(new NewSinkResult(removedChestsCount.get(), itemNames)));
            } catch (Throwable t) {
                future.complete(Optional.empty());
                throw t;
            }
        }, locations);

        return future;
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
    public boolean removeChest(Location... locations) {
        processNetworkChanges(() -> {
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
                chestDatabase.removeChest(networkID, locations);
            }
        }, locations);

        return true;
    }

    @Override
    public Optional<Integer> getNetworkFromSourceChest(Block... blocks) {
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
                    if (!(state instanceof Chest)) {
                        removeChest(location);
                        continue;
                    }

                    Inventory destInv = ((Chest) state).getInventory();

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

    private CompletableFuture<Boolean> loadNetworks(List<Integer> networkIDs) {
        CompletableFuture<List<CompletableFuture<Integer>>> future = new CompletableFuture<>();

        CommandBook.server().getScheduler().runTaskAsynchronously(CommandBook.inst(), () -> {
            Collection<PixieNetworkDefinition> networkDefinitions = chestDatabase.getChestsInNetworks(networkIDs).get();

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

            // Mark this future complete so that we can wait
            future.complete(futures);
        });

        return future.thenCompose((listOfFutures) -> {
            try {
                // Wait for the futures
                CompletableFuture.allOf(listOfFutures.toArray(new CompletableFuture[0])).get();

                return CompletableFuture.completedFuture(true);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();

                return CompletableFuture.completedFuture(false);
            }
        });
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
            Collection<Integer> networksInChunk = chestDatabase.getNetworksInChunk(chunk).get();

            networkChunkRefCountLock.lock();
            try {
                List<Integer> networksToLoad = new ArrayList<>();

                for (Integer networkID : networksInChunk) {
                    boolean alreadyLoaded = networkChunkRefCount.contains(networkID);
                    networkChunkRefCount.increment(networkID);

                    if (!alreadyLoaded) {
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
            Collection<Integer> networksInChunk = chestDatabase.getNetworksInChunk(chunk).get();

            networkChunkRefCountLock.lock();
            try {
                List<Integer> networksToUnload = new ArrayList<>();

                for (Integer networkID : networksInChunk) {
                    networkChunkRefCount.decrement(networkID);
                    boolean networkStillLoaded = networkChunkRefCount.contains(networkID);

                    if (!networkStillLoaded) {
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
