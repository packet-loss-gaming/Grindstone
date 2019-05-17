/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.homes;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.events.HomeTeleportEvent;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.Optional;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Ender Pearl Homes", desc = "Teleport with enderpearls!")
public class EnderPearlHomesComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private HomeManager homeManager;

    @Override
    public void enable() {
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        File homeDirectory = new File(inst.getDataFolder().getPath() + "/home");
        if (!homeDirectory.exists()) homeDirectory.mkdir();

        HomeDatabase homeDatabase = new CSVHomeDatabase("homes", homeDirectory);
        homeDatabase.load();

        homeManager = new HomeManager(homeDatabase);
    }

    public Location getBedLocation(Player player) {
        return homeManager.getPlayerHome(player).orElse(null);
    }

    public Location getRespawnLocation(Player player) {
        Location respawnLoc = player.getWorld().getSpawnLocation();
        return getBedLocation(player) != null ? getBedLocation(player) : respawnLoc;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity ent = event.getEntity();

        if (ent instanceof Enderman) {
            event.getDrops().add(new ItemStack(Material.ENDER_PEARL, ChanceUtil.getRandom(12)));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        Location to = event.getTo();

        if (!inst.hasPermission(player, "aurora.home.enderpearl")
                || !event.getCause().equals(TeleportCause.ENDER_PEARL)) return;
        try {
            if (to.distanceSquared(player.getLocation()) < 1.5 * 1.5) {
                ChatUtil.sendNotice(player, "A powerful vortex sucks you up!");
                Optional<Location> playerHomeLoc = homeManager.getPlayerHome(player);
                if (!playerHomeLoc.isPresent() || playerHomeLoc.get().getBlock().getType() != Material.BED_BLOCK) {
                    event.setCancelled(true);
                    server.getScheduler().scheduleSyncDelayedTask(inst, () -> {
                        if (!player.teleport(player.getWorld().getSpawnLocation())) {
                            return;
                        }
                        ChatUtil.sendNotice(player, "The vortex cannot find your home and sends you to spawn.");
                    }, 1);
                } else {
                    event.setCancelled(true);
                    server.getScheduler().scheduleSyncDelayedTask(inst, () -> {
                        HomeTeleportEvent HTE = new HomeTeleportEvent(player, getBedLocation(player));
                        server.getPluginManager().callEvent(HTE);
                        if (HTE.isCancelled()) {
                            return;
                        }

                        if (!player.teleport(HTE.getDestination())) {
                            return;
                        }

                        ChatUtil.sendNotice(player, "The vortex sends you to your home.");
                    }, 1);
                }
            }
        } catch (Exception e) {
            log.warning("The player: " + player.getName() + "'s teleport could not be processed by the: "
                    + this.getInformation().friendlyName() + " component.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        HomeTeleportEvent HTE = new HomeTeleportEvent(event.getPlayer(), getRespawnLocation(event.getPlayer()));
        server.getPluginManager().callEvent(HTE);
        if (!HTE.isCancelled()) event.setRespawnLocation(HTE.getDestination());
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        Location bedLoc = event.getBed().getLocation();

        if (player == null || bedLoc == null) return;
        if (player.getWorld().getName().toLowerCase().contains("legit")) return;

        homeManager.setPlayerHomeAndNotify(player, bedLoc);
    }
}