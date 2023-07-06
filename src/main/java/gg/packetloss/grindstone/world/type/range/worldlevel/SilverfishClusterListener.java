/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import gg.packetloss.grindstone.util.region.RegionWalker;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.List;

class SilverfishClusterListener implements Listener {
    private final WorldLevelComponent parent;

    public SilverfishClusterListener(WorldLevelComponent parent) {
        this.parent = parent;
    }

    private static final List<Material> TRIGGER_BLOCKS = List.of(Material.STONE, Material.DEEPSLATE);

    private void createSilverfishCluster(World world, BlockVector3 location) {
        CuboidRegion region = new CuboidRegion(
            location.subtract(-1, -1, -1),
            location.add(1, 1, 1)
        );

        RegionWalker.walk(region, (x, y, z) -> {
            if (!(world.getMinHeight() <= y && y < world.getMaxHeight())) {
                return;
            }

            Block block = world.getBlockAt(x, y, z);
            switch (block.getType()) {
                case STONE -> block.setType(Material.INFESTED_STONE);
                case DEEPSLATE -> block.setType(Material.INFESTED_DEEPSLATE);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreakWatch(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!TRIGGER_BLOCKS.contains(block.getType())) {
            return;
        }

        World world = block.getWorld();
        if (!parent.isRangeWorld(world)) {
            return;
        }

        Player player = event.getPlayer();
        int level = parent.getWorldLevel(player);
        WorldLevelConfig config = parent.getConfig();
        if (level < config.mobsSilverfishLevelEnabledAt) {
            return;
        }

        int chanceOfSpawning = Math.max(
            config.mobsSilverfishMaxChance,
            config.mobsSilverfishBaseChance - level
        );
        if (!ChanceUtil.getChance(chanceOfSpawning)) {
            return;
        }

        if (ChanceUtil.getChance(config.mobsSilverfishInitialSilverfishChance)) {
            world.spawn(block.getLocation().add(.5, 0, .5), Silverfish.class);
        }

        Bukkit.getScheduler().runTaskLater(
            CommandBook.inst(),
            () -> createSilverfishCluster(world, WorldEditBridge.toBlockVec3(block)),
            1
        );
    }
}
