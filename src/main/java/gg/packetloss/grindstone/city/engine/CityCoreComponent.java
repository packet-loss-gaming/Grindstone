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
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.material.Door;

import java.util.logging.Logger;

@ComponentInformation(friendlyName = "City Core", desc = "Operate the core city functionality.")
@Depend(components = {AdminComponent.class, HomeManagerComponent.class})
public class CityCoreComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private AdminComponent admin;
    @InjectComponent
    private HomeManagerComponent homeManager;

    @Override
    public void enable() {
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    private boolean isCityWorld(World world) {
        return world.getName().equals("City");
    }

    private boolean isRangeWorld(World world) {
        return world.getName().equals("Halzeil");
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        final Player player = event.getPlayer();
        final Location from = getConsistentFrom(event.getFrom());

        World fromWorld = from.getWorld();

        switch (event.getCause()) {
            case NETHER_PORTAL: {
                // City Code
                if (isCityWorld(fromWorld)) {
                    World world = Bukkit.getWorld("Halzeil");
                    Location targetLoc = new Location(world, from.getX() * 64, from.getY(), from.getZ() * 64);
                    event.setTo(targetLoc);
                    return;
                } else if (isRangeWorld(fromWorld)) {
                    World world = Bukkit.getWorld("City");
                    Location targetLoc = new Location(world, from.getX() / 64, from.getY(), from.getZ() / 64);
                    event.setTo(targetLoc);
                    return;
                }
                break;
            }
        }
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
