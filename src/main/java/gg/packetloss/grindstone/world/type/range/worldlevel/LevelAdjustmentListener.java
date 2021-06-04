/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

class LevelAdjustmentListener implements Listener {
    private WorldLevelComponent parent;
    private Set<UUID> recentlyAwardedPlayers = new HashSet<>();

    public LevelAdjustmentListener(WorldLevelComponent parent) {
        this.parent = parent;
    }

    private boolean wasRecentlyAwarded(Player player) {
        return recentlyAwardedPlayers.contains(player.getUniqueId());
    }

    private void markPlayerRecentlyAwarded(Player player) {
        recentlyAwardedPlayers.add(player.getUniqueId());

        CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
            recentlyAwardedPlayers.remove(player.getUniqueId());
        }, 30);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteractBlock(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        World world = block.getWorld();
        if (!parent.isRangeWorld(world)) {
            return;
        }

        if (block.getType() == Material.DRAGON_EGG) {
            // Always stop the interaction
            event.setCancelled(true);

            // If the player was recently awarded, hold off, give them time to process that this is here
            Player player = event.getPlayer();
            if (wasRecentlyAwarded(player)) {
                return;
            }

            // Remove the block, and create a fake explosion for fun
            block.setType(Material.AIR);
            ExplosionStateFactory.createFakeExplosion(block.getLocation());

            // Update the world level
            int newLevel = parent.getWorldLevel(player) + 1;
            parent.setWorldLevel(player, newLevel);
            parent.showTitleForLevel(player, newLevel);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        World world = event.getBlock().getWorld();
        if (!parent.isRangeWorld(world)) {
            return;
        }

        Block block = event.getBlock();
        Location blockLoc = block.getLocation();
        if (parent.shouldSpawnChallengeBlock(blockLoc, block.getType())) {
            parent.spawnChallengeBlock(blockLoc);

            Player player = event.getPlayer();
            markPlayerRecentlyAwarded(player);
        }
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        World world = player.getWorld();
        if (!parent.isRangeWorld(world) || parent.isRangeWorld(event.getFrom())) {
            return;
        }

        parent.showTitleForLevelIfInteresting(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Location respawnLocation = event.getRespawnLocation();
        if (!parent.isRangeWorld(respawnLocation.getWorld())) {
            return;
        }

        Player player = event.getPlayer();
        parent.showTitleForLevelIfInteresting(player);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        World world = player.getWorld();
        if (!parent.isRangeWorld(world)) {
            return;
        }

        parent.setWorldLevel(player, parent.getWorldLevel(player) / 2);
    }
}
