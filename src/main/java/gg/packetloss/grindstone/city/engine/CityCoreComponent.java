/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
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
public class CityCoreComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        final Player player = event.getPlayer();
        final Location pLoc = player.getLocation().clone();
        final Location from = event.getFrom();

        World fromWorld = from.getWorld();

        switch (event.getCause()) {
            case NETHER_PORTAL: {
                TravelAgent agent = event.getPortalTravelAgent();

                // City Code
                if (isCityWorld(fromWorld)) {

                    pLoc.setWorld(Bukkit.getWorld("Halzeil"));
                    pLoc.setX(pLoc.getBlockX() * 128);
                    pLoc.setZ(pLoc.getBlockZ() * 128);
                    agent.setCanCreatePortal(true);

                    event.useTravelAgent(true);
                    event.setPortalTravelAgent(agent);

                    event.setTo(agent.findOrCreate(pLoc));
                    return;
                } else if (isRangeWorld(fromWorld)) {

                    pLoc.setWorld(Bukkit.getWorld("City"));
                    pLoc.setX(pLoc.getBlockX() / 128);
                    pLoc.setZ(pLoc.getBlockZ() / 128);
                    agent.setCanCreatePortal(false);

                    event.useTravelAgent(true);
                    event.setPortalTravelAgent(agent);

                    event.setTo(agent.findOrCreate(pLoc));
                    return;
                }
                break;
            }
        }
    }
}
