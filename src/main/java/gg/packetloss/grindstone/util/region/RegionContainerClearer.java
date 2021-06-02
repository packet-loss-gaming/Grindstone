/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.region;

import com.sk89q.worldedit.regions.Region;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;

public class RegionContainerClearer {
    private void walkBlock(World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        BlockState blockState = block.getState();
        if (blockState instanceof Container) {
            ((Container) blockState).getSnapshotInventory().clear();
            blockState.update();
        }
    }

    public void walkRegion(Region region, World world) {
        RegionWalker.walk(region, (x, y, z) -> {
            walkBlock(world, x, y, z);
        });
    }
}
