package gg.packetloss.grindstone.state;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@ComponentInformation(friendlyName = "Player State", desc = "Player state management")
public class PlayerStateComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

    @InjectComponent
    private NativeSerializerComponent serializer;

    private Path statesDir;

    private Gson gson = new Gson();

    private Map<UUID, CompletableFuture<PlayerStateRecord>> loadingRecords = new ConcurrentHashMap<>();

    private Map<UUID, PlayerStateRecord> recordMapping = new ConcurrentHashMap<>();
    private Map<UUID, List<ItemStack>> inventoryCache = new ConcurrentHashMap<>();

    @Override
    public void enable() {
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        try {
            Path baseDir = Path.of(inst.getDataFolder().getPath(), "state");
            statesDir = Files.createDirectories(baseDir.resolve("states"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Path getStateRecord(UUID playerID) {
        return statesDir.resolve(playerID.toString() + ".json");
    }

    private PlayerStateRecord loadStateRecord(UUID playerID) throws IOException {
        // Create a new future, cancelling any current futures
        CompletableFuture<PlayerStateRecord> future = loadingRecords.compute(playerID, (ignored, storedFuture) -> {
            if (storedFuture != null) {
                storedFuture.cancel(true);
            }

            return new CompletableFuture<>();
        });

        Path path = getStateRecord(playerID);
        if (!Files.exists(path)) {
            PlayerStateRecord record = new PlayerStateRecord();
            recordMapping.put(playerID, record);
            future.complete(record);
            return record;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            PlayerStateRecord record = gson.fromJson(reader, PlayerStateRecord.class);

            // Load associated inventories
            for (UUID associatedInvID : record.getInventories().values()) {
                inventoryCache.put(associatedInvID, serializer.readItems(associatedInvID));
            }

            // Add the record (this must happen after inventories are loaded so anything requiring a state record
            // find its respective inventory).
            recordMapping.put(playerID, record);

            future.complete(record);
            return record;
        } catch (IOException e) {
            future.completeExceptionally(e);
            throw e;
        }
    }

    private PlayerStateRecord requireStateRecord(UUID playerID) throws IOException {
        PlayerStateRecord record = recordMapping.get(playerID);
        if (record != null) {
            return record;
        }

        CompletableFuture<PlayerStateRecord> loadingRecord = loadingRecords.get(playerID);
        if (loadingRecord != null) {
            return loadingRecord.join();
        }

        // It wasn't found, one of two things happened:

        // 1. It was found between us checking / race condition
        record = recordMapping.get(playerID);
        if (record != null) {
            return record;
        }

        // 2. This connection was before the event listener was registered.
        return loadStateRecord(playerID);
    }

    private void writeStateRecord(UUID playerID) throws IOException {
        Path stateRecordPath = getStateRecord(playerID);

        PlayerStateRecord record = requireStateRecord(playerID);
        if (record.isEmpty()) {
            Files.deleteIfExists(stateRecordPath);
        } else {
            try (BufferedWriter writer = Files.newBufferedWriter(stateRecordPath, StandardOpenOption.CREATE)) {
                writer.write(gson.toJson(record));
            }
        }
    }

    private void unloadStateRecord(UUID playerID) {
        recordMapping.remove(playerID);
    }

    public boolean hasValidStoredState(PlayerStateType type, Player player) {
        try {
            PlayerStateRecord record = requireStateRecord(player.getUniqueId());

            if (type.hasVitals() && record.getVitals().get(type.name()) == null) {
                return false;
            }

            if (type.hasInventory() && record.getInventories().get(type.name()) == null) {
                return false;
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void pushState(PlayerStateType type, Player player) throws IOException {
        PlayerStateRecord record = requireStateRecord(player.getUniqueId());

        if (type.hasVitals()) {
            record.getVitals().put(type.name(), new PlayerVitals(
                    player.getHealth(),
                    player.getFoodLevel(),
                    player.getSaturation(),
                    player.getExhaustion(),
                    player.getTotalExperience()
            ));
        }

        if (type.hasInventory()) {
            UUID inventoryID = UUID.randomUUID();
            record.getInventories().put(type.name(), inventoryID);

            List<ItemStack> stacks = Lists.newArrayList(ItemUtil.clone(player.getInventory().getContents()));
            inventoryCache.put(inventoryID, stacks);

            serializer.writeItems(inventoryID, stacks);
        }

        writeStateRecord(player.getUniqueId());
    }

    public void popState(PlayerStateType type, Player player) throws IOException {
        PlayerStateRecord record = requireStateRecord(player.getUniqueId());

        if (type.hasVitals()) {
            PlayerVitals vitals = record.getVitals().remove(type.name());
            if (vitals != null) {
                player.setHealth(Math.min(player.getMaxHealth(), vitals.getHealth()));
                player.setFoodLevel(vitals.getHunger());
                player.setSaturation(vitals.getSaturation());
                player.setExhaustion(vitals.getExhaustion());
                player.setTotalExperience(vitals.getExperience());
            }
        }

        if (type.hasInventory()) {
            UUID inventory = record.getInventories().remove(type.name());
            if (inventory != null) {
                List<ItemStack> contents = inventoryCache.remove(inventory);

                player.getInventory().setContents(contents.toArray(new ItemStack[0]));
                player.updateInventory();

                serializer.removeItems(inventory);
            }
        }

        writeStateRecord(player.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        try {
            loadStateRecord(event.getUniqueId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        unloadStateRecord(event.getPlayer().getUniqueId());
    }
}
