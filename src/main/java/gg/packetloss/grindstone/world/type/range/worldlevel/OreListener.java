/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel;

import gg.packetloss.grindstone.events.entity.item.DropClearPulseEvent;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class OreListener implements Listener {
    private WorldLevelComponent parent;
    private PlayerPlacedOresState oresState;

    public OreListener(WorldLevelComponent parent, PlayerPlacedOresState oresState) {
        this.parent = parent;
        this.oresState = oresState;
    }

    private boolean isOreMultiplied(BlockState block) {
        if (!EnvironmentUtil.isOre(block.getType())) {
            return false;
        }

        return !oresState.isBlockPlayerPlaced(block.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        BlockState block = event.getBlock().getState();

        if (!parent.isRangeWorld(player.getWorld())) {
            return;
        }

        if (EnvironmentUtil.isOre(block.getType())) {
            oresState.markPlayerPlaced(block.getBlock());
        }
    }

    private long nextDropTime = 0;

    @EventHandler
    public void onDropClearPulse(DropClearPulseEvent event) {
        if (parent.isRangeWorld(event.getWorld())) {
            nextDropTime = System.currentTimeMillis() + (event.getSecondsLeft() * 1000);
        }
    }

    private int getOreMod(int level) {
        return (int) Math.max(1, (level * 3));
    }

    private void addPool(Player player, Location destination, int times, ItemStack drop) {
        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        taskBuilder.setNumberOfRuns(times);

        taskBuilder.setInterval(20);
        taskBuilder.setDelay(20);

        World world = destination.getWorld();
        float vol = ((float) 1 / times);
        taskBuilder.setAction((timesL) -> {
            if (nextDropTime != 0 && System.currentTimeMillis() < nextDropTime) {
                return false;
            }

            EntityUtil.spawnProtectedItem(drop.clone(), player, destination);
            world.playSound(destination, Sound.ENTITY_BLAZE_AMBIENT, Math.min(1, (((float) timesL / times) * .6F) + vol), 0);
            return true;
        });
        taskBuilder.setFinishAction(() -> {
            world.playSound(destination, Sound.ENTITY_BLAZE_DEATH, .2F, 0);
        });

        taskBuilder.build();
    }

    private void tryAddPool(Player player, BlockState block) {
        int level = parent.getWorldLevel(player);
        if (level < 2) {
            return;
        }

        Location destination = block.getLocation().add(0.5, 0, .5);
        int times = ChanceUtil.getRandom(getOreMod(level));
        ItemStack drop = EnvironmentUtil.getOreDrop(block.getType(), player.getItemInHand());

        addPool(player, destination, times, drop);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreakWatch(BlockBreakEvent event) {
        Player player = event.getPlayer();
        BlockState block = event.getBlock().getState();
        if (!parent.isRangeWorld(player.getWorld())) {
            return;
        }

        if (isOreMultiplied(block)) {
            tryAddPool(player, block);
        } else if (EnvironmentUtil.isOre(block.getType())) {
            oresState.clearPlayerPlacement(block.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!parent.isRangeWorld(player.getWorld())) {
            return;
        }

        int level = parent.getWorldLevel(player);
        event.setExpToDrop(event.getExpToDrop() * level);
    }

    private boolean containsOre(List<Block> blockList) {
        for (Block block : blockList) {
            if (EnvironmentUtil.isOre(block.getType())) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        if (containsOre(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPiston(BlockPistonRetractEvent event) {
        if (containsOre(event.getBlocks())) {
            event.setCancelled(true);
        }
    }
}
