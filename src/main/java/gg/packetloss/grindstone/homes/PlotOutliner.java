/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.homes;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Map;

public class PlotOutliner {
    private final Map<Material, Material> mapping;

    public PlotOutliner(Map<Material, Material> mapping) {
        this.mapping = mapping;
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

            for (Map.Entry<Material, Material> entry : mapping.entrySet()) {
                Material from;
                Material to;

                if (!revert) {
                    from = entry.getKey();
                    to = entry.getValue();
                } else {
                    from = entry.getValue();
                    to = entry.getKey();
                }

                if (target.getType().equals(from)) {
                    target.setType(to, true);
                    return;
                }
            }
        }
    }
}
