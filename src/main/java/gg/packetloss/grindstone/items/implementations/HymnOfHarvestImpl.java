/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import gg.packetloss.grindstone.events.custom.item.HymnSingEvent;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.util.particle.SingleBlockParticleEffect;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.util.region.RegionWalker;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ItemSpawnEvent;

public class HymnOfHarvestImpl extends AbstractItemFeatureImpl {
    private final int MAX_RADIUS = 5;

    private Player harvestingPlayer = null;

    private void harvestOuterRegion(Player player, World world, CuboidRegion region, CuboidRegion innerRegion) {
        RegionWalker.walk(region, (x, y, z) -> {
            if (innerRegion.contains(BlockVector3.at(x, y, z))) {
                return;
            }

            Block block = world.getBlockAt(x, y, z);
            Location blockLoc = block.getLocation();

            // Check for line of sight
            if (!player.hasLineOfSight(blockLoc)) {
                return;
            }

            // Spawn a star so players know something is happening
            SingleBlockParticleEffect.randomStar(blockLoc);

            // If the crop isn't ready yet skip it
            BlockData blockData = block.getBlockData();
            if (!(blockData instanceof Ageable)) {
                return;
            }

            // Only harvest fully grown blocks
            if (((Ageable) blockData).getAge() != ((Ageable) blockData).getMaximumAge()) {
                return;
            }

            // Check build permissions
            BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
            CommandBook.callEvent(breakEvent);
            if (breakEvent.isCancelled()) {
                return;
            }

            // Break the block
            block.breakNaturally();

            // Spawn more particles at the changed block and play a sound
            SingleBlockParticleEffect.burstOfStars(blockLoc);
            world.playSound(blockLoc, Sound.ITEM_CROP_PLANT, 1, 1);

            // Automatically replant
            ((Ageable) blockData).setAge(0);
            block.setBlockData(blockData);
        });
    }

    private void spawnHarvest(Player player, Location origin) {
        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        taskBuilder.setInterval(1);
        taskBuilder.setNumberOfRuns(MAX_RADIUS);

        World world = origin.getWorld();
        int x = origin.getBlockX();
        int y = origin.getBlockY();
        int z = origin.getBlockZ();

        taskBuilder.setAction((times) -> {
            int currentRadius = MAX_RADIUS - times;
            int currentInnerRadius = currentRadius - 1;

            CuboidRegion region = new CuboidRegion(
                BlockVector3.at(x - currentRadius, y - currentRadius, z - currentRadius),
                BlockVector3.at(x + currentRadius, y + currentRadius, z + currentRadius)
            );
            CuboidRegion innerRegion = new CuboidRegion(
                BlockVector3.at(x - currentInnerRadius, y - currentInnerRadius, z - currentInnerRadius),
                BlockVector3.at(x + currentInnerRadius, y + currentInnerRadius, z + currentInnerRadius)
            );

            harvestingPlayer = player;
            try {
                harvestOuterRegion(player, world, region, innerRegion);
            } finally {
                harvestingPlayer = null;
            }

            return true;
        });

        taskBuilder.build();
    }

    @EventHandler(ignoreCancelled = true)
    public void onHymnSing(HymnSingEvent event) {
        Player player = event.getPlayer();
        HymnSingEvent.Hymn hymn = event.getHymn();

        if (hymn != HymnSingEvent.Hymn.HARVEST) {
            return;
        }

        spawnHarvest(player, player.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDropItemEvent(ItemSpawnEvent event) {
        Player player = harvestingPlayer;
        if (player == null || !player.isValid()) {
            return;
        }

        Item item = event.getEntity();
        GeneralPlayerUtil.giveItemToPlayer(player, item.getItemStack());

        event.setCancelled(true);
    }
}
