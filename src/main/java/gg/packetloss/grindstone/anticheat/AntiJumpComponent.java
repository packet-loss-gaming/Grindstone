/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.anticheat;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

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

            final Location blockLoc = event.getBlock().getLocation();
            final int blockY = blockLoc.getBlockY();

            if (Math.abs(player.getVelocity().getY()) > config.upwardsVelocity && playerLoc.getY() > blockY) {
                server.getScheduler().runTaskLater(inst, () -> {
                    if (player.getLocation().getY() >= (blockY + config.leapDistance)) {

                        if (LocationUtil.distanceSquared2D(playerLoc, blockLoc) > config.radius * config.radius) {
                            return;
                        }

                        ChatUtil.sendWarning(player, "Hack jumping detected.");

                        playerLoc.setY(blockY);

                        player.teleport(playerLoc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
                    }
                }, 4);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        // We're only interested in cancelled events for the purposes of fixing the client
        // side prediction.
        if (!event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        Location startingLocation = player.getLocation();

        // We don't care about flight disabling, only enabling.
        if (!event.isFlying()) {
            return;
        }

        server.getScheduler().runTaskLater(inst, () -> {
            player.teleport(LocationUtil.findFreePosition(startingLocation, false), PlayerTeleportEvent.TeleportCause.UNKNOWN);
        }, 20);
    }
}
