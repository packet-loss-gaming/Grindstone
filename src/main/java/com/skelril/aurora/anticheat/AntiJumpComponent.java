/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.anticheat;

import com.sk89q.commandbook.CommandBook;
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Anti Jump", desc = "Stop the jump hackers")
public class AntiJumpComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private LocalConfiguration config;

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("upwards-velocity")
        public double upwardsVelocity = .1;
        @Setting("leap-distance")
        public double leapDistance = 1.2;
        @Setting("radius")
        public double radius = 2;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {

        if (event.isCancelled()) {

            final Player player = event.getPlayer();
            final Location playerLoc = player.getLocation();

            final double x = playerLoc.getX();
            final double y = playerLoc.getY();
            final double z = playerLoc.getZ();

            final Location blockLoc = event.getBlock().getLocation();
            final int blockY = blockLoc.getBlockY();

            if (Math.abs(player.getVelocity().getY()) > config.upwardsVelocity && y > blockY) {
                server.getScheduler().runTaskLater(inst, new Runnable() {

                    @Override
                    public void run() {

                        if (player.getLocation().getY() >= (blockY + config.leapDistance)) {

                            Location basePlayerLoc = playerLoc.clone();
                            basePlayerLoc.setY(0);
                            Location baseBlockLoc = blockLoc.clone();
                            baseBlockLoc.setY(0);

                            if (basePlayerLoc.distanceSquared(baseBlockLoc) > config.radius * config.radius) {
                                return;
                            }

                            ChatUtil.sendWarning(player, "Hack jumping detected.");

                            Location playerLoc = player.getLocation();
                            playerLoc.setX(x);
                            playerLoc.setY(blockY);
                            playerLoc.setZ(z);

                            player.teleport(playerLoc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
                        }
                    }
                }, 4);
            }
        }
    }
}
