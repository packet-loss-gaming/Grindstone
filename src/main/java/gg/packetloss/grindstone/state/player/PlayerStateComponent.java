package gg.packetloss.grindstone.state.player;

import com.google.gson.Gson;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePopEvent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePushEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.state.player.attribute.TypedPlayerStateAttribute;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
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
    private PlayerStatePersistenceManager cacheManager;

    @Override
    public void enable() {
        cacheManager = new PlayerStatePersistenceManager(serializer);

        try {
            Path baseDir = Path.of(inst.getDataFolder().getPath(), "state");
            statesDir = Files.createDirectories(baseDir.resolve("states/players"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
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
                cacheManager.loadInventory(associatedInvID);
            }

            // Add the record (this must happen after inventories are loaded so anything requiring a state record
            // find its respective inventory).
            recordMapping.put(playerID, record);

            future.complete(record);
            return record;
        } catch (Throwable t) {
            future.completeExceptionally(t);
            throw t;
        }
    }

    private PlayerStateRecord requireStateRecord(UUID playerID, boolean loadIfEarly) throws IOException {
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
        if (loadIfEarly) {
            return loadStateRecord(playerID);
        }

        return null;
    }

    private PlayerStateRecord requireStateRecord(UUID playerID) throws IOException {
        return requireStateRecord(playerID, true);
    }

    private void writeStateRecord(UUID playerID) throws IOException {
        Path stateRecordPath = getStateRecord(playerID);

        PlayerStateRecord record = requireStateRecord(playerID);
        if (record.isEmpty()) {
            Files.deleteIfExists(stateRecordPath);
        } else {
            try (BufferedWriter writer = Files.newBufferedWriter(
                    stateRecordPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                writer.write(gson.toJson(record));
            }
        }
    }

    private void unloadStateRecord(UUID playerID) throws IOException {
        requireStateRecord(playerID, false);

        PlayerStateRecord record = recordMapping.remove(playerID);
        if (record != null) {
            for (UUID associatedInvID : record.getInventories().values()) {
                cacheManager.loadInventory(associatedInvID);
            }
        }
    }

    public boolean hasValidStoredState(PlayerStateKind kind, Player player) {
        try {
            PlayerStateRecord record = requireStateRecord(player.getUniqueId());

            // If something is checking, but we're in the process of removing the state, consider it already removed.
            Optional<PlayerStateKind> kindBeingRemoved = record.getKindBeingRemoved();
            if (kindBeingRemoved.orElse(null) == kind) {
                return false;
            }

            for (TypedPlayerStateAttribute attribute : kind.getAttributes()) {
                if (attribute.isValidFor(record)) {
                    return true;
                }
            }

            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public void pushState(PlayerStateKind kind, Player player) throws IOException, ConflictingPlayerStateException {
        // Automatically handle any conflicts with temporary inventories
        if (!kind.allowUseWithTemporaryState()) {
            tryPopTempKind(player);
        }

        PlayerStateRecord record = requireStateRecord(player.getUniqueId());

        record.pushKind(kind);

        for (TypedPlayerStateAttribute attribute : kind.getAttributes()) {
            attribute.getWorkerFor(cacheManager).pushState(record, player);
        }

        server.getPluginManager().callEvent(new PlayerStatePushEvent(player, kind));

        writeStateRecord(player.getUniqueId());
    }

    public void popState(PlayerStateKind kind, Player player) throws IOException {
        PlayerStateRecord record = requireStateRecord(player.getUniqueId());

        record.beginPopKind(kind);

        for (TypedPlayerStateAttribute attribute : kind.getAttributes()) {
            attribute.getWorkerFor(cacheManager).popState(record, player);
        }

        record.finishPopKind(kind);

        server.getPluginManager().callEvent(new PlayerStatePopEvent(player, kind));

        writeStateRecord(player.getUniqueId());
    }

    public boolean hasTempKind(Player player) throws IOException {
        PlayerStateRecord record = requireStateRecord(player.getUniqueId());
        return record.getTempKind().isPresent();
    }

    public void tryPopTempKind(Player player) throws IOException {
        PlayerStateRecord record = requireStateRecord(player.getUniqueId());
        Optional<PlayerStateKind> optTempKind = record.getTempKind();
        if (optTempKind.isEmpty()) {
            return;
        }

        popState(optTempKind.get(), player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) throws IOException {
        loadStateRecord(event.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) throws IOException {
        unloadStateRecord(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        server.getScheduler().runTask(inst, () -> {
            try {
                tryPopTempKind(event.getPlayer());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws IOException {
        Player player = event.getPlayer();
        if (player.isDead()) {
            return;
        }

        tryPopTempKind(player);
    }
}
