package gg.packetloss.grindstone.util.region;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.util.RegionUtil;
import gg.packetloss.grindstone.util.functional.TriPredicate;
import org.apache.logging.log4j.util.TriConsumer;

public class RegionWalker {
    public static void walk(ProtectedRegion region, TriConsumer<Integer, Integer, Integer> op) {
        walk(RegionUtil.convert(region).orElseThrow(), op);
    }

    public static void walk(Region region, TriConsumer<Integer, Integer, Integer> op) {
        testWalk(region, (x, y, z) -> {
            op.accept(x, y, z);
            return false;
        });
    }

    public static void testWalk(ProtectedRegion region, TriPredicate<Integer, Integer, Integer> op) {
        testWalk(RegionUtil.convert(region).orElseThrow(), op);
    }

    public static void testWalk(Region region, TriPredicate<Integer, Integer, Integer> op) {
        final BlockVector3 min = region.getMinimumPoint();
        final BlockVector3 max = region.getMaximumPoint();

        int minX = min.getBlockX();
        int minY = min.getBlockY();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxY = max.getBlockY();
        int maxZ = max.getBlockZ();

        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    if (op.test(x, y, z)) {
                        return;
                    }
                }
            }
        }
    }
}
