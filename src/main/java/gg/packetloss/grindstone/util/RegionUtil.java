/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.util.region.RegionValueEvaluator;
import gg.packetloss.grindstone.util.region.RegionValueReport;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RegionUtil {
    public static Location getCenterAt(World world, double y, ProtectedRegion region) {
        Region rg = new CuboidRegion(region.getMinimumPoint(), region.getMaximumPoint().add(1, 1, 1));
        Vector3 center = rg.getCenter();
        return new Location(world, center.getX(), y, center.getZ());
    }

    public static Location getCenter(World world, ProtectedRegion region) {
        Region rg = new CuboidRegion(region.getMinimumPoint(), region.getMaximumPoint().add(1, 1, 1));
        Vector3 center = rg.getCenter();
        return new Location(world, center.getX(), center.getY(), center.getZ());
    }

    public static boolean isLoaded(World world, ProtectedRegion region) {
        BlockVector3 min = region.getMinimumPoint();
        if (!world.isChunkLoaded(min.getBlockX() >> 4, min.getBlockZ() >> 4)) {
            return false;
        }

        BlockVector3 max = region.getMaximumPoint();
        if (!world.isChunkLoaded(max.getBlockX() >> 4, max.getBlockZ() >> 4)) {
            return false;
        }

        return true;
    }

    public static Optional<Region> convert(ProtectedRegion region) {
        if (region instanceof ProtectedCuboidRegion) {
            ProtectedCuboidRegion cuboid = (ProtectedCuboidRegion)region;
            BlockVector3 pt1 = cuboid.getMinimumPoint();
            BlockVector3 pt2 = cuboid.getMaximumPoint();
            return Optional.of(new CuboidRegion(pt1, pt2));
        } else if (region instanceof ProtectedPolygonalRegion) {
            ProtectedPolygonalRegion poly2d = (ProtectedPolygonalRegion)region;
            return Optional.of(new Polygonal2DRegion(
                    null, poly2d.getPoints(),
                    poly2d.getMinimumPoint().getBlockY(),
                    poly2d.getMaximumPoint().getBlockY())
            );
        } else {
            return Optional.empty();
        }
    }

    public static Stream<ProtectedRegion> getHouseStream(LocalPlayer player, RegionManager manager) {
        return manager.getRegions().entrySet().stream()
                .filter(e -> e.getValue().getOwners().contains(player))
                .filter(e -> e.getKey().endsWith("-house"))
                .map(Map.Entry::getValue);
    }

    public static List<ProtectedRegion> getHouses(LocalPlayer player, RegionManager manager) {
        return getHouseStream(player, manager).collect(Collectors.toList());
    }

    public static Optional<Integer> sumChunks(List<ProtectedRegion> regions) {
        int total = 0;

        for (ProtectedRegion house : regions) {
            Optional<Integer> chunkCount = countChunks(house);
            if (chunkCount.isEmpty()) {
                return Optional.empty();
            }
            total += chunkCount.get();
        }

        return Optional.of(total);
    }

    public static Optional<Integer> getXWidth(Region region) {
        if (region instanceof CuboidRegion) {
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();

            int minX = min.getBlockX();
            int maxX = max.getBlockX();

            return Optional.of(Math.abs(maxX - minX) + 1);

        }
        return Optional.empty();
    }

    public static Optional<Integer> getZWidth(Region region) {
        if (region instanceof CuboidRegion) {
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();

            int minZ = min.getBlockZ();
            int maxZ = max.getBlockZ();

            return Optional.of(Math.abs(maxZ - minZ) + 1);
        }
        return Optional.empty();
    }

    public static Optional<Integer> countChunks(Region region) {
        if (region instanceof CuboidRegion) {
            int xWidth = getXWidth(region).orElseThrow();
            int zWidth = getZWidth(region).orElseThrow();

            return Optional.of((xWidth * zWidth) / (16 * 16));
        }
        return Optional.empty();
    }

    public static Optional<Integer> countChunks(ProtectedRegion region) {
        Optional<Region> convertedRegion = convert(region);
        if (convertedRegion.isPresent()) {
            return countChunks(convertedRegion.get());
        }

        return Optional.empty();
    }

    public static double calcChunkPrice(double chunkCount) {
        return Math.pow(chunkCount, 4) * (chunkCount / 2);
    }

    public static CompletableFuture<Double> calcBlockPrice(Region region, World world) {
        try {
            return new RegionValueEvaluator(false)
                    .walkRegion(region, world)
                    .thenApply(RegionValueReport::getBlockPrice);
        } catch (UnsupportedOperationException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    public static CompletableFuture<Optional<Double>> calcBlockPrice(ProtectedRegion region, World world) {
        Optional<Region> convertedRegion = convert(region);
        if (convertedRegion.isPresent()) {
            return calcBlockPrice(convertedRegion.get(), world).thenApply(Optional::of);
        }

        return CompletableFuture.completedFuture(Optional.empty());
    }

    public static CompletableFuture<Optional<Double>> getPrice(Region region, World world, boolean commission) {
        CompletableFuture<Optional<Double>> future = new CompletableFuture<>();

        Optional<Integer> size = countChunks(region);
        if (size.isEmpty()) {
            future.complete(Optional.empty());
            return future;
        }

        CompletableFuture<Double> blockPriceFuture = calcBlockPrice(region, world);
        blockPriceFuture.thenAccept((blockPrice) -> {
            double chunkPrice = calcChunkPrice(size.get());
            double total = chunkPrice + blockPrice;
            if (commission) {
                total *= 1.1;
            }
            future.complete(Optional.of(total));
        });

        return future;
    }

    public static boolean renameRegion(RegionManager manager, String oldName, String newName, boolean cleanPersonal)
        throws ProtectedRegion.CircularInheritanceException, StorageException {

        // Check for old conflicting regions
        ProtectedRegion oldRegion = manager.getRegion(oldName);
        ProtectedRegion newRegion = manager.getRegion(newName);

        if (oldRegion == null || newRegion != null) {
            return false;
        }

        // Recreate the region based on it's old values
        if (oldRegion instanceof ProtectedPolygonalRegion) {
            int minY = oldRegion.getMinimumPoint().getBlockY();
            int maxY = oldRegion.getMaximumPoint().getBlockY();
            newRegion = new ProtectedPolygonalRegion(newName, oldRegion.getPoints(), minY, maxY);
        } else if (oldRegion instanceof ProtectedCuboidRegion) {
            BlockVector3 min = oldRegion.getMinimumPoint();
            BlockVector3 max = oldRegion.getMaximumPoint();
            newRegion = new ProtectedCuboidRegion(newName, min, max);
        } else {
            return false;
        }

        // Assign the old values to the new region
        if (!cleanPersonal) {
            newRegion.setMembers(oldRegion.getMembers());
            newRegion.setOwners(oldRegion.getOwners());
            newRegion.setFlags(oldRegion.getFlags());
        }
        newRegion.setPriority(oldRegion.getPriority());
        newRegion.setParent(oldRegion.getParent());

        // Remove the old region and add the new region then proceed to attempt to save the regions
        // Save twice because databases can be funky
        manager.removeRegion(oldRegion.getId());
        manager.save();
        manager.addRegion(newRegion);
        manager.save();
        return true;
    }
}
