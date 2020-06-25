package gg.packetloss.grindstone.world.timetravel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.ComponentCommandRegistrar;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldIsQuery;
import gg.packetloss.grindstone.world.managed.ManagedWorldTimeContext;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ComponentInformation(friendlyName = "Time Travel", desc = "Sometimes focusing on the past is good.")
public class TimeTravelComponent extends BukkitComponent {
    @InjectComponent
    private ManagedWorldComponent managedWorlds;

    private Path statesDir;

    private Map<UUID, ManagedWorldTimeContext> timeContextOverride = new HashMap<>();

    @Override
    public void enable() {
        try {
            Path baseDir = Path.of(CommandBook.inst().getDataFolder().getPath(), "state");
            statesDir = Files.createDirectories(baseDir.resolve("states"));

            loadTimeTravelState();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        CommandBook.registerEvents(new TimeTravelListener(this));

        ComponentCommandRegistrar registrar = CommandBook.getComponentRegistrar();
        TimeContextConverter.register(registrar);
        registrar.registerTopLevelCommands((commandManager, registration) -> {
            registration.register(commandManager, TimeTravelCommandsRegistration.builder(), new TimeTravelCommands(this));
        });
    }

    @Override
    public void disable() {
        saveTimeTravelState();
    }

    private Path getStateFile() {
        return statesDir.resolve("time-travel.json");
    }

    private void loadTimeTravelState() {
        Path stateFile = getStateFile();
        if (!Files.exists(stateFile)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(stateFile)) {
            Type overrideMapType = new TypeToken<Map<UUID, ManagedWorldTimeContext>>() { }.getType();

            timeContextOverride = new Gson().fromJson(reader, overrideMapType);
        } catch (IOException e) {
            CommandBook.logger().warning("Failed to load previous time travel state");
            e.printStackTrace();
        }
    }

    private void saveTimeTravelState() {
        Path stateFile = getStateFile();
        try (BufferedWriter writer = Files.newBufferedWriter(
                stateFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            Type overrideMapType = new TypeToken<Map<UUID, ManagedWorldTimeContext>>() { }.getType();
            writer.write(new Gson().toJson(timeContextOverride, overrideMapType));
        } catch (IOException e) {
            CommandBook.logger().warning("Failed to save previous weather state");
            e.printStackTrace();
        }
    }

    public ManagedWorldTimeContext getTimeContextFor(Player player) {
        ManagedWorldTimeContext override = timeContextOverride.get(player.getUniqueId());
        if (override != null) {
            return override;
        }

        return managedWorlds.getTimeContextFor(player.getWorld());
    }

    public void resetOverride(Player player) {
        timeContextOverride.remove(player.getUniqueId());
    }

    public void setOverride(Player player, ManagedWorldTimeContext timeContext) {
        if (timeContext == ManagedWorldTimeContext.LATEST) {
            resetOverride(player);
            return;
        }

        timeContextOverride.put(player.getUniqueId(), timeContext);
    }

    protected boolean canUseTimeTravelCommand(Player player) {
        if (managedWorlds.is(ManagedWorldIsQuery.ANY_RANGE, player.getWorld())) {
            return false;
        }
        return true;
    }

    public void maybeUpdateOverride(Player player, World newWorld) {
        if (!managedWorlds.is(ManagedWorldIsQuery.ANY_RANGE, newWorld)) {
            return;
        }

        ManagedWorldTimeContext timeContext = managedWorlds.getTimeContextFor(newWorld);
        if (timeContext == ManagedWorldTimeContext.LATEST) {
            resetOverride(player);
        } else {
            setOverride(player, timeContext);
        }
    }

    public void maybeUpdateOverride(Player player) {
        maybeUpdateOverride(player, player.getWorld());
    }
}
