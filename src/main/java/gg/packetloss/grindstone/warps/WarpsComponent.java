/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.warps;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.click.ClickType;
import gg.packetloss.grindstone.events.DoubleClickEvent;
import gg.packetloss.grindstone.events.HomeTeleportEvent;
import gg.packetloss.grindstone.events.PortalRecordEvent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

import static gg.packetloss.grindstone.click.ClickRecord.TICKS_FOR_DOUBLE_CLICK;

@ComponentInformation(
        friendlyName = "Rift Warps",
        desc = "Provides warps functionality"
)
public class WarpsComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private WarpManager warpManager;

    protected WarpManager getWarpManager() {
        return warpManager;
    }

    @Override
    public void enable() {
        CommandBook.registerEvents(this);

        CommandBook.getComponentRegistrar().registerTopLevelCommands((registrar) -> {
            WarpPointConverter.register(registrar, this);

            registrar.register(WarpCommandsRegistration.builder(), new WarpCommands(this));

            registrar.registerAsSubCommand("warps", "Warp management", (warpsRegistrar) -> {
                warpsRegistrar.register(WarpManagementCommandsRegistration.builder(), new WarpManagementCommands(this));
            });
        });

        File warpsDirectory = new File(inst.getDataFolder().getPath() + "/warps");
        if (!warpsDirectory.exists()) warpsDirectory.mkdir();

        WarpDatabase warpDatabase = new CSVWarpDatabase("warps", warpsDirectory);
        warpDatabase.load();

        warpManager = new WarpManager(warpDatabase);
    }

    public Optional<Location> getRawBedLocation(Player player) {
        return warpManager.getHomeFor(player).map(WarpPoint::getLocation);
    }

    public Optional<Location> getBedLocation(Player player) {
        return warpManager.getHomeFor(player).map(WarpPoint::getSafeLocation);
    }

    public Optional<Location> getLastPortalLocation(Player player, World world) {
        return warpManager.getLastPortalLocationFor(player, world).map(WarpPoint::getSafeLocation);
    }

    public Location getRespawnLocation(Player player) {
        Location spawnLoc = player.getWorld().getSpawnLocation();
        return getBedLocation(player).orElse(spawnLoc);
    }

    public Optional<Location> getWarp(WarpQualifiedName warpName) {
        return warpManager.getExactWarp(warpName).map(WarpPoint::getSafeLocation);
    }

    // FIXME: Priority set as workaround for Multiverse-Core#1977
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        HomeTeleportEvent HTE = new HomeTeleportEvent(event.getPlayer(), getRespawnLocation(event.getPlayer()));
        server.getPluginManager().callEvent(HTE);
        if (!HTE.isCancelled()) event.setRespawnLocation(HTE.getDestination());
    }

    private void recordPortal(Player player, Location location) {
        PortalRecordEvent event = new PortalRecordEvent(player, location);

        CommandBook.callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        warpManager.setLastPortalLocation(player, location);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPortal(PlayerTeleportEvent event) {
        // Use this over the nether portal check, because we need to pay attention to redirects.
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        World fromWorld = from.getWorld();
        World toWorld = to.getWorld();

        World.Environment fromEnvironment = fromWorld.getEnvironment();
        World.Environment toEnvironment = toWorld.getEnvironment();

        // Do not record world changes that involve a nether. This may be reconsidered in the future.
        if (fromEnvironment == World.Environment.NETHER || toEnvironment == World.Environment.NETHER) {
            return;
        }

        Player player = event.getPlayer();

        // Invert the view location to make walking through portals feel more natural.
        Location invertedViewLocation = from.clone();
        invertedViewLocation.setDirection(from.getDirection().multiply(-1).setY(0));

        recordPortal(player, invertedViewLocation);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        event.setCancelled(true);
    }

    private List<UUID> rightClickedOnBedPlayers = new ArrayList<>();

    @EventHandler(ignoreCancelled = true)
    public void onPlayerClickBed(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (!EnvironmentUtil.isBed(block)) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        rightClickedOnBedPlayers.add(player.getUniqueId());

        queueRespawnMechanicNotification(player);
    }

    private void queueRespawnMechanicNotification(Player player) {
        CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
            if (!rightClickedOnBedPlayers.contains(player.getUniqueId())) {
                return;
            }

            ChatUtil.sendNotice(player, "Double right click to set respawn point.");
            rightClickedOnBedPlayers.remove(player.getUniqueId());
        }, TICKS_FOR_DOUBLE_CLICK + 1);
    }

    @EventHandler
    public void onDoubleRightClickBed(DoubleClickEvent event) {
        if (event.getClickType() != ClickType.RIGHT) {
            return;
        }

        Block block = event.getAssociatedBlock();
        if (block == null || !EnvironmentUtil.isBed(block)) {
            return;
        }

        Player player = event.getPlayer();
        warpManager.setPlayerHomeAndNotify(player, block.getLocation());

        rightClickedOnBedPlayers.removeAll(Collections.singleton(player.getUniqueId()));
    }
}
