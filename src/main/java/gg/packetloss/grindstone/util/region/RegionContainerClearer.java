package gg.packetloss.grindstone.util.region;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.CuboidRegion;
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
        //noinspection ConstantConditions
        if (region instanceof CuboidRegion) {
            Vector min = region.getMinimumPoint();
            Vector max = region.getMaximumPoint();

            int minX = min.getBlockX();
            int minY = min.getBlockY();
            int minZ = min.getBlockZ();
            int maxX = max.getBlockX();
            int maxY = max.getBlockY();
            int maxZ = max.getBlockZ();

            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    for (int z = minZ; z <= maxZ; ++z) {
                        walkBlock(world, x, y, z);
                    }
                }
            }
        } else {
            throw new UnsupportedOperationException("Unsupported region type");
        }
    }
}
