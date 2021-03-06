/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.region;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.util.RegionUtil;
import gg.packetloss.grindstone.util.functional.TriConsumer;
import gg.packetloss.grindstone.util.functional.TriPredicate;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class RegionWalker {
    private static void testWalkInBounds(Region region, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                                         TriPredicate<Integer, Integer, Integer> op) {
        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    // Check region containment for non-cuboid shapes
                    if (region.contains(BlockVector3.at(x, y, z)) && op.test(x, y, z)) {
                        return;
                    }
                }
            }
        }
    }

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

        testWalkInBounds(region, minX, minY, minZ, maxX, maxY, maxZ, op);
    }

    public static void walkChunks(ProtectedRegion region, BiConsumer<Integer, Integer> op) {
        walkChunks(RegionUtil.convert(region).orElseThrow(), op);
    }

    public static void walkChunks(Region region, BiConsumer<Integer, Integer> op) {
        testWalkChunks(region, (x, z) -> {
            op.accept(x, z);
            return false;
        });
    }

    public static void testWalkChunks(ProtectedRegion region, BiPredicate<Integer, Integer> op) {
        testWalkChunks(RegionUtil.convert(region).orElseThrow(), op);
    }

    public static void testWalkChunks(Region region, BiPredicate<Integer, Integer> op) {
        final BlockVector3 min = region.getMinimumPoint();
        final BlockVector3 max = region.getMaximumPoint();

        int minX = min.getBlockX();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();

        for (int x = minX; x <= maxX; x += 16) {
            for (int z = minZ; z <= maxZ; z += 16) {
                if (op.test(x >> 4, z >> 4)) {
                    return;
                }
            }
        }
    }

    public static void walkInChunk(ProtectedRegion region, int chunkX, int chunkZ,
                                   TriConsumer<Integer, Integer, Integer> op) {
        walkInChunk(RegionUtil.convert(region).orElseThrow(), chunkX, chunkZ, op);
    }

    public static void walkInChunk(Region region, int chunkX, int chunkZ, TriConsumer<Integer, Integer, Integer> op) {
        testWalkInChunk(region, chunkX, chunkZ, (x, y, z) -> {
            op.accept(x, y, z);
            return false;
        });
    }
    public static void testWalkInChunk(ProtectedRegion region, int chunkX, int chunkZ,
                                       TriPredicate<Integer, Integer, Integer> op) {
        testWalkInChunk(RegionUtil.convert(region).orElseThrow(), chunkX, chunkZ, op);
    }

    public static void testWalkInChunk(Region region, int chunkX, int chunkZ,
                                       TriPredicate<Integer, Integer, Integer> op) {
        final BlockVector3 min = region.getMinimumPoint();
        final BlockVector3 max = region.getMaximumPoint();

        int chunkMinX = Math.min(chunkX << 4, (chunkX << 4) + 16);
        int chunkMaxX = Math.max(chunkX << 4, (chunkX << 4) + 16);
        int chunkMinZ = Math.min(chunkZ << 4, (chunkZ << 4) + 16);
        int chunkMaxZ = Math.max(chunkZ << 4, (chunkZ << 4) + 16);

        int minX = Math.max(min.getBlockX(), chunkMinX);
        int minY = min.getBlockY();
        int minZ = Math.max(min.getBlockZ(), chunkMinZ);
        int maxX = Math.min(max.getBlockX(), chunkMaxX);
        int maxY = max.getBlockY();
        int maxZ = Math.min(max.getBlockZ(), chunkMaxZ);

        testWalkInBounds(region, minX, minY, minZ, maxX, maxY, maxZ, op);
    }
}
