/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.events.custom.item.BuildToolUseEvent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.listener.DoorRestorationListener;
import gg.packetloss.grindstone.util.listener.NaturalSpawnBlockingListener;
import gg.packetloss.grindstone.util.listener.NuisanceSpawnBlockingListener;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldIsQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;

@ComponentInformation(friendlyName = "City Core", desc = "Operate the core city functionality.")
@Depend(components = {AdminComponent.class, ManagedWorldComponent.class})
public class CityCoreComponent extends BukkitComponent implements Listener {
    @InjectComponent
    private AdminComponent admin;
    @InjectComponent
    private ManagedWorldComponent managedWorld;

    private LocalConfiguration config;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        // Process the configuration 1 tick late to make sure all worlds are loaded at the time of processing.
        Bukkit.getScheduler().runTask(CommandBook.inst(), this::processConfig);

        CommandBook.registerEvents(this);

        CommandBook.registerEvents(new DoorRestorationListener(this::isCityWorld));
        CommandBook.registerEvents(new NaturalSpawnBlockingListener(this::isCityWorld));
        CommandBook.registerEvents(new NuisanceSpawnBlockingListener(this::isCityWorld));
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
        processConfig();
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("world-border.size")
        public double worldBorderSize = 3250;
    }

    private boolean isCityWorld(World world) {
        return managedWorld.is(ManagedWorldIsQuery.CITY, world);
    }

    public void setWorldBoarder() {
        for (World world : CommandBook.server().getWorlds()) {
            if (!isCityWorld(world)) {
                continue;
            }

            WorldBorder worldBorder = world.getWorldBorder();
            worldBorder.setSize(config.worldBorderSize);
            worldBorder.setCenter(world.getSpawnLocation());
        }
    }

    private void processConfig() {
        setWorldBoarder();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortalForm(PortalCreateEvent event) {
        if (event.getReason().equals(PortalCreateEvent.CreateReason.FIRE)) return;
        if (isCityWorld(event.getWorld())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBuildToolUse(BuildToolUseEvent event) {
        if (admin.isAdmin(event.getPlayer())) {
            return;
        }

        Location startingPoint = event.getStartingPoint();

        if (!isCityWorld(startingPoint.getWorld())) {
            return;
        }

        Player player = event.getPlayer();
        ChatUtil.sendError(player, "The city council has decided this tool shouldn't be used here.");
        event.setCancelled(true);
    }
}
