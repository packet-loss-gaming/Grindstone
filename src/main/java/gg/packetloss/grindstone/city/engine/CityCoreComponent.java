/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.events.custom.item.BuildToolUseEvent;
import gg.packetloss.grindstone.homes.HomeManagerComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.warps.WarpsComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.material.Door;

import java.util.logging.Logger;

@ComponentInformation(friendlyName = "City Core", desc = "Operate the core city functionality.")
@Depend(components = {AdminComponent.class, HomeManagerComponent.class, WarpsComponent.class})
public class CityCoreComponent extends BukkitComponent implements Listener {
    private static final String CITY_WORLD = "City";
    private static final String PRIMARY_RANGE_WORLD = "Halzeil";

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private AdminComponent admin;
    @InjectComponent
    private HomeManagerComponent homeManager;
    @InjectComponent
    private WarpsComponent warps;

    @Override
    public void enable() {
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    private boolean isCityWorld(World world) {
        return world.getName().equals(CITY_WORLD);
    }

    private boolean isRangeWorld(World world) {
        return world.getName().equals(PRIMARY_RANGE_WORLD);
    }

    public World getCityWorld() {
        return CommandBook.server().getWorld(CITY_WORLD);
    }

    public World getCurrentRangeWorld() {
        return CommandBook.server().getWorld(PRIMARY_RANGE_WORLD);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDoorBreak(EntityBreakDoorEvent event) {
        Block block = event.getBlock();

        if (!isCityWorld(block.getWorld())) {
            return;
        }

        // Open the door.
        server.getScheduler().runTaskLater(inst, () -> {
            BlockState state = block.getRelative(BlockFace.DOWN).getState();
            Door doorData = (Door) state.getData();
            doorData.setOpen(true);
            state.update(true);
        }, 1);

        // Prevent the door from being destroyed.
        event.setCancelled(true);
    }

    private Location getConsistentFrom(Location loc) {
        Location xTestLoc = loc.clone();

        while (true) {
            if (xTestLoc.getBlock().getType() == Material.NETHER_PORTAL) {
                xTestLoc.add(-1, 0, 0);
                continue;
            }

            break;
        }

        Location zTestLoc = loc.clone();
        while (true) {
            if (zTestLoc.getBlock().getType() == Material.NETHER_PORTAL) {
                zTestLoc.add(0, 0, -1);
                continue;
            }

            break;
        }

        return new Location(loc.getWorld(), xTestLoc.getBlockX() + 1, loc.getY(), zTestLoc.getBlockZ() + 1);
    }

    public Location getNewPlayerStartingLocation(Player player) {
        return getCurrentRangeWorld().getSpawnLocation();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        final Player player = event.getPlayer();
        final Location from = getConsistentFrom(event.getFrom());

        World fromWorld = from.getWorld();

        switch (event.getCause()) {
            case NETHER_PORTAL: {
                // City Code
                if (isCityWorld(fromWorld)) {
                    Location location = warps
                            .getLastPortalLocation(player, getCurrentRangeWorld())
                            .orElseGet(() -> getNewPlayerStartingLocation(player));

                    event.setCancelled(true);
                    player.teleport(location, PlayerTeleportEvent.TeleportCause.NETHER_PORTAL);
                    return;
                } else if (isRangeWorld(fromWorld)) {
                    Location location = warps
                            .getLastPortalLocation(player, getCityWorld())
                            .orElseGet(() -> getCityWorld().getSpawnLocation());

                    event.setCancelled(true);
                    player.teleport(location, PlayerTeleportEvent.TeleportCause.NETHER_PORTAL);
                    return;
                }
                break;
            }
        }
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

        if (homeManager.isInAnyPlayerHome(startingPoint)) {
            return;
        }

        Player player = event.getPlayer();
        ChatUtil.sendError(player, "The city council has decided this tool shouldn't be used here.");
        event.setCancelled(true);
    }
}
