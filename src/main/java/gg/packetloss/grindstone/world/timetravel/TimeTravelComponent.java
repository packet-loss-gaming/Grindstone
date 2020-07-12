package gg.packetloss.grindstone.world.timetravel;

import com.google.gson.reflect.TypeToken;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.ComponentCommandRegistrar;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.persistence.SingleFileFilesystemStateHelper;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldIsQuery;
import gg.packetloss.grindstone.world.managed.ManagedWorldTimeContext;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ComponentInformation(friendlyName = "Time Travel", desc = "Sometimes focusing on the past is good.")
public class TimeTravelComponent extends BukkitComponent {
    @InjectComponent
    private ManagedWorldComponent managedWorlds;

    private Map<UUID, ManagedWorldTimeContext> timeContextOverride = new HashMap<>();
    private SingleFileFilesystemStateHelper<Map<UUID, ManagedWorldTimeContext>> stateHelper;

    @Override
    public void enable() {
        try {
            stateHelper = new SingleFileFilesystemStateHelper<>("time-travel.json", new TypeToken<>() {});
            stateHelper.load().ifPresent(loadedState -> timeContextOverride = loadedState);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        CommandBook.registerEvents(new TimeTravelListener(this));

        ComponentCommandRegistrar registrar = CommandBook.getComponentRegistrar();
        registrar.registerTopLevelCommands((commandManager, registration) -> {
            TimeContextConverter.register(commandManager);

            registration.register(commandManager, TimeTravelCommandsRegistration.builder(), new TimeTravelCommands(this));
        });
    }

    @Override
    public void disable() {
        try {
            stateHelper.save(timeContextOverride);
        } catch (IOException e) {
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
