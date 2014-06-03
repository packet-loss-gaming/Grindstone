/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.arena.factory;

import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.city.engine.arena.AbstractRegionedArena;
import com.skelril.aurora.util.EnvironmentUtil;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class LavaSupply extends AbstractRegionedArena {

    private ProtectedRegion lava;

    public LavaSupply(World world, ProtectedRegion region, ProtectedRegion lava) {
        super(world, region);
        this.lava = lava;
    }

    // Returns remainder
    public int addLava(int amount) {

        com.sk89q.worldedit.Vector min = lava.getMinimumPoint();
        com.sk89q.worldedit.Vector max = lava.getMaximumPoint();

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
                        Block block = getWorld().getBlockAt(x, y, z);
                        if (block.getType() == Material.AIR) {
                            block.setTypeIdAndData(BlockID.STATIONARY_LAVA, (byte) 0, false);
                            ++added;
                        }
                    }
                }
            }
        }
        return amount - added;
    }

    // Returns amount removed
    public int removeLava(int amount) {

        com.sk89q.worldedit.Vector min = lava.getMinimumPoint();
        com.sk89q.worldedit.Vector max = lava.getMaximumPoint();

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
                    if (found < amount) {
                        Block block = getWorld().getBlockAt(x, y, z);
                        if (EnvironmentUtil.isLava(block)) {
                            block.setTypeIdAndData(0, (byte) 0, false);
                            ++found;
                        }
                    }
                }
            }
        }
        return found;
    }
}
