/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.timetravel;

import com.google.gson.reflect.TypeToken;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.persistence.SingleFileFilesystemStateHelper;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldTimeContext;
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

        CommandBook.getComponentRegistrar().registerTopLevelCommands((registrar) -> {
            TimeContextConverter.register(registrar);

            registrar.register(TimeTravelCommandsRegistration.builder(), new TimeTravelCommands(this));
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

    public ManagedWorldTimeContext getSelectedArchivePeriod(Player player) {
        ManagedWorldTimeContext override = timeContextOverride.get(player.getUniqueId());
        if (override != null) {
            return override;
        }

        return ManagedWorldTimeContext.getLatestArchive();
    }

    public void resetOverride(Player player) {
        timeContextOverride.remove(player.getUniqueId());
    }

    public void setOverride(Player player, ManagedWorldTimeContext timeContext) {
        if (timeContext.ordinal() >= ManagedWorldTimeContext.getLatestArchive().ordinal()) {
            resetOverride(player);
            return;
        }

        timeContextOverride.put(player.getUniqueId(), timeContext);
    }
}
