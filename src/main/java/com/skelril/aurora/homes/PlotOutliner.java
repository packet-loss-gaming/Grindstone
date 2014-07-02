/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.homes;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.util.checker.Expression;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.Map;

public class PlotOutliner {

    private final Map<BaseBlock, BaseBlock> mapping;
    private final Expression<BaseBlock, Boolean> expr;

    public PlotOutliner(Map<BaseBlock, BaseBlock> mapping, Expression<BaseBlock, Boolean> expr) {
        this.mapping = mapping;
        this.expr = expr;
    }

    public void outline(World world, ProtectedRegion region) {
        edit(world, region, false);
    }

    public void revert(World world, ProtectedRegion region) {
        edit(world, region, true);
    }

    private void edit(World world, ProtectedRegion region, boolean revert) {
        BlockVector min = region.getMinimumPoint();
        BlockVector max = region.getMaximumPoint();

        for (int x = min.getBlockX(); x <= max.getBlockX(); ++x) {
            setBlock(world, revert, x, min.getBlockZ());
            setBlock(world, revert, x, max.getBlockZ());
        }

        for (int z = min.getBlockZ(); z <= max.getBlockZ(); ++z) {
            setBlock(world, revert, min.getBlockX(), z);
            setBlock(world, revert, max.getBlockX(), z);
        }
    }

    private void setBlock(World world, boolean revert, int x, int z) {
        for (int y = world.getMaxHeight(); y > 1; --y) {

            Block target = world.getBlockAt(x, y, z);
            Block below = target.getRelative(BlockFace.DOWN);

            BaseBlock tBase = new BaseBlock(target.getTypeId(), target.getData());
            BaseBlock bBase = new BaseBlock(below.getTypeId(), below.getData());

            for (Map.Entry<BaseBlock, BaseBlock> entry : mapping.entrySet()) {
                BaseBlock from;
                BaseBlock to;
                if (!revert) {
                    from = entry.getKey();
                    to = entry.getValue();
                } else {
                    from = entry.getValue();
                    to = entry.getKey();
                }

                if (tBase.equals(from) && expr.evaluate(bBase)) {
                    target.setTypeIdAndData(to.getType(), (byte) to.getData(), true);
                    return;
                }
            }
        }
    }
}
