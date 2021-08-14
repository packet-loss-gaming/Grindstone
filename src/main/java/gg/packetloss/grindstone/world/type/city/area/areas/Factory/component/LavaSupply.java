/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Factory.component;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.CollectionUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.region.RegionWalker;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

public class LavaSupply {
    private final World world;
    private final ProtectedRegion[] lavaChannels;
    private final ProtectedRegion lava;

    private int numDamaged = 0;
    private int lavaCount = 0;

    public LavaSupply(World world, ProtectedRegion[] lavaChannels, ProtectedRegion lava) {
        this.world = world;
        this.lavaChannels = lavaChannels;
        this.lava = lava;

        this.lavaCount = countLava();
    }

    public void checkDamage() {
        numDamaged = 0;
        for (ProtectedRegion lavaChannel : lavaChannels) {
            RegionWalker.walk(lavaChannel, (x, y, z) -> {
                if (world.getBlockAt(x, y, z).getType() != Material.IRON_BLOCK) {
                    ++numDamaged;
                }
            });
        }
    }

    public boolean tryDamageRandomChannelBlock() {
        Block block = LocationUtil.pickLocation(world, CollectionUtil.getElement(lavaChannels)).getBlock();
        if (block.getType() != Material.IRON_BLOCK) {
            return false;
        }

        if (!block.getRelative(BlockFace.DOWN).getType().isAir()) {
            return false;
        }

        block.setType(Material.AIR);
        world.dropItem(block.getLocation(), new ItemStack(Material.IRON_INGOT, ChanceUtil.getRandom(9)));

        return true;
    }

    public boolean tryAddLava() {
        if (!ChanceUtil.getChance(numDamaged * 3)) {
            return false;
        }

        // If we fail to add lava/it's full, don't damage the pipeline
        if (addLava(1) != 0) {
            return false;
        }

        if (ChanceUtil.getChance(10)) {
            // Try up to three times to damage a block, increase the number of damaged blocks to prevent
            // forcing a rewalk
            for (int i = 0; i < 3; ++i) {
                if (tryDamageRandomChannelBlock()) {
                    ++numDamaged;
                    break;
                }
            }
        }

        return true;
    }

    // Returns amount of lava available for removal
    private int countLava() {
        BlockVector3 min = lava.getMinimumPoint();
        BlockVector3 max = lava.getMaximumPoint();

        int minX = min.getBlockX();
        int minZ = min.getBlockZ();
        int minY = min.getBlockY();
        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();
        int maxY = max.getBlockY();

        int found = 0;
        for (int y = maxY; y >= minY; --y) {
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    Block block = world.getBlockAt(x, y, z);
                    if (EnvironmentUtil.isLava(block)) {
                        ++found;
                    }
                }
            }
        }
        return found;
    }

    public int getLavaCount() {
        return lavaCount;
    }

    // Returns remainder
    public int addLava(int amount) {
        BlockVector3 min = lava.getMinimumPoint();
        BlockVector3 max = lava.getMaximumPoint();

        int minX = min.getBlockX();
        int minZ = min.getBlockZ();
        int minY = min.getBlockY();
        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();
        int maxY = max.getBlockY();

        int added = 0;
        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    if (added < amount) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType() != Material.AIR) {
                            continue;
                        }

                        // Hack to work around an issue where this adds lava into the
                        // supply line causing items to not flow into the spot where they
                        // to be for input detection.
                        if (EnvironmentUtil.isWater(block.getRelative(BlockFace.DOWN))) {
                            continue;
                        }

                        block.setType(Material.LAVA, false);
                        ++added;
                    }
                }
            }
        }

        lavaCount += added;

        return amount - added;
    }

    // Returns amount removed
    public int removeLava(int amount) {
        BlockVector3 min = lava.getMinimumPoint();
        BlockVector3 max = lava.getMaximumPoint();

        int minX = min.getBlockX();
        int minZ = min.getBlockZ();
        int minY = min.getBlockY();
        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();
        int maxY = max.getBlockY();

        int removed = 0;
        for (int y = maxY; y >= minY; --y) {
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    if (removed < amount) {
                        Block block = world.getBlockAt(x, y, z);
                        if (EnvironmentUtil.isLava(block)) {
                            block.setType(Material.AIR, false);
                            ++removed;
                        }
                    }
                }
            }
        }

        lavaCount -= removed;

        return removed;
    }
}
