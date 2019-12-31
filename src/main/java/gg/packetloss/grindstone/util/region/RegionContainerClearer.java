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
